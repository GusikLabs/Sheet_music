/*
* Подключаем пакет express, который позволяет нам поднять http сервер
* Слушаем 3000 порт на сервера
*/
const express = require('express')
const app = express()
const port = 3000

/*
* Подключаем пакет для работы с браузером (открыть страницу/нажать на кнопку и т.д.)
* Вытягиваем движок хрома
*/
const { chromium } = require('playwright');

// Подключаем пакет для работы с файловой системой
const fs = require('fs');

// Подключаем пакет для возможности отправлять http запросы
const axios = require('axios');

/*
* Указываем имя бакета на firebase
* Создаем переменную srcFilename которая будет хранить название файла на firebase
* По умолчанию инициализируем ее music.mp3
* Создаем константу destFilename которая хранит название файла, который мы
* назовем при скачке файла с firebase - иным словами имя временного файла на сервере
*/
const bucketName = 'my-application2-80cdd.appspot.com';
let srcFilename = 'music.mp3';
const destFilename = 'music2.mp3';

// Подключаем пакет для работы с firebase и тянем сущность Storage
const {Storage} = require('@google-cloud/storage');

// Подлючаем пакт для работы с PDF документами
const PDFDocument = require('pdfkit');

/*
* Создаем и инициализируем Storage
* Задаем projectId взятый с firebase и
* файл с конфиги скачанный с firebase
*/
const storage = new Storage({
	projectId: "my-application2-80cdd",
	keyFilename: './account.json'
});

/*
* Создаем функцию для скачивания файла с firebase
* Пакет firebase имеет асинхронные функции, по-этому помечаем метод асинхронным
*/
async function downloadFile (srcFilename, destFilename) {
	await storage.bucket(bucketName).file(srcFilename).download({ destination: destFilename });
}

/*
* Создаем функцию для загрузки файла на firebase
*/
async function uploadFile (path) {
	await storage.bucket(bucketName).upload(path);
}

/*
* Создаем функцию для загрузки изображения с нотами после конвертации
*/
const download_image = (url, image_path) =>

  // Посылаем запрос на url и говорим что типом ответа будет поток
  axios({
    url,
    responseType: 'stream',
  }).then(
    response =>

    // Создаем promise (ф-цию), который запишет поток в путь image_path
      new Promise((resolve, reject) => {
        response.data
          .pipe(fs.createWriteStream(image_path))
          .on('finish', () => resolve())
          .on('error', e => reject(e));
      }),
  );


// Создаем функцию для конвертации midi -> sheets
async function convertToMidi (browser, res) {
	try
	{
	    // Задаем контекст выполнения браузера, в котором разрешаем скачивание
		const context = await browser.newContext({acceptDownloads: true });

		// Создаем страницу, и переходим по url
		const page = await context.newPage();
		let url = 'https://solmire.com';
		await page.goto(`${url}/miditosheetmusic/`);

         // Вешаем обработчик на событие выбора файла на странице
		page.on('filechooser', async (fileChooser) => {
			console.log('OPEN DIALOG WINDOW');

			// Говорим что загружаем файл result.midi
			await fileChooser.setFiles('./result.midi');
			console.log('FILE UPLOAD');

			// Нажимаем на кнопку конвертации
			page.click('//*[@id="main_content"]/div/form/table[2]/tbody/tr/td[3]/input');
			console.log('CLICK CONVERT');

            // Ожидаем пока появится кнопка со скачиванием
            // timeout - 1000000, такой большой, из-за того что могут загрузить файлы разной длины
			page.waitForSelector('.page', { timeout: 1000000 }).then(async () => {
			    // Создаем массив со всеми изображениями нот на странице
				let images = await page.$$('.page img');

				// Задаем имя папки куда сохранятся наши изображения с нотами
                let folder = './' + (new Date()).getTime();
                // Создаем папку
                fs.mkdirSync(folder);

                // Создаем объект для работы с PDF документами
                const doc = new PDFDocument();
                // Задаем имя будущему pdf файлу с нотами
                let outputFile = 'output' + (new Date()).getTime() + '.pdf';
                // Открываем поток на запись в файл
                doc.pipe(fs.createWriteStream(`./${folder}/${outputFile}`));

                // Проходимся циклам по всем элементами массива с изображениями
                for (let i = 0; i < images.length; ++i)
                	{
                		// Вытягиваем у ноды атрибут src - то-есть ссылка к файла с изображением
                		let src = await images[i].getAttribute('src')
                		// Начинаем скачивать изображение
                		await download_image(url + src, `${folder}/${i}.png`).then(() => {
                		// Добавляем новую страницу в PDF документ
                		// И размещаем на ней изображение с нотами
                			doc.addPage().image(`${folder}/${i}.png`, {
                				fit: [900, 600],
                				});
                			});
                	}

                // Закрываем PDF документ
                doc.end();
                // Загружаем pdf файл на firebase
                uploadFile(`./${folder}/${outputFile}`);
                // Закрываем браузер
                browser.close();
                // Отправляем http ответ, что все хорошо и путь pdf файла на firebase
                res.json({'result': '1', 'path': outputFile});
            });
        });

        // Нажимаем на кнопку выбора файла
        await page.click('.file');
             }
                 catch (error) {
                	// Отлавливаем ошибки и выводим в консоль
                	console.log(error);
                }
             }

    // Устанавливаем серверу роутинг /api/convert, при котором сработает ф-ция для конвертирования mp3 -> midi
    app.get('/api/convert', async (req, res) => {

    // Проверяем чтоб был передам ?path
    if (req.query.path == undefined) {
        // Если его нет, отправляем http ответ с ошибкой
        res.json({'result': '0', 'desc': 'path_not_found'});
        return;
    }
    // В переменную srcFilename сохраняем наш путь к файлу на firebase
    srcFilename = req.query.path;

    // Создаем объект браузера на основе движка хрома
    const browser = await chromium.launch({
         headless: true
    });
    // Задаем контекст выполнения браузера, в котором разрешаем скачивание
    const context = await browser.newContext({acceptDownloads: true });
    // Создаем страницу
    const page = await context.newPage();
    // Переходим на сайт по url
    await page.goto('https://www.ofoct.com/audio-converter/convert-wav-or-mp3-ogg-aac-wma-to-midi.html');

    // Вешаем обрабочтик на событие - выбора файла
    page.on('filechooser', async (fileChooser) => {
         console.log('OPEN DIALOG WINDOW');
         // Скачиваем mp3 файл в firebase
         await downloadFile(srcFilename, destFilename);
         // Говорим что загружаем скаченный mp3 с сервера
         await fileChooser.setFiles(destFilename);
         console.log('FILE UPLOAD');
         // Нажимаем на кнопку конвертации
         page.click('#convet_do');
         console.log('CLICK CONVERT');
         // Ждем пока пройдет конвертация и появится кнопка для скачивания
         // timeout - 1000000, такой большой, из-за того что могут загрузить файлы разной длины
         page.waitForSelector('.download_a', { timeout: 1000000 }).then(async () => {
              console.log('READY');
              // Загружаем наш midi файл
              const [ download ] = await Promise.all([
                   page.waitForEvent('download'),
                   page.click('.download_a')
              ]);
              const path = await download.path();

              // Так как он загрузится в папку Temp, то перещаем его в нужную нам папку
              fs.rename(path, './result.midi', async function (err) {
              // Если ошибка - кидаем исключением
                   if (err) throw err
                   console.log('Successfully moved!')
                   // Загружаем midi файл на firebase
                   uploadFile('./result.midi');
                   // Запускаем конвертацию midi -> sheets
                   convertToMidi(browser, res);
              });
         });
    });

    // Нажимаем на кнопку загрузки песни
    await page.click('.ajax-upload-dragdrop');

});
// Запускаем сервер и указываем что слушаем порт из переменной port
app.listen(port);