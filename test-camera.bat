@echo off
echo ========================================
echo MediaShift Kamera Test Script
echo ========================================
echo.

echo Java versiyonu kontrol ediliyor...
java -version
if %errorlevel% neq 0 (
    echo HATA: Java kurulu değil!
    echo Lütfen Java 17+ kurun ve tekrar deneyin.
    pause
    exit /b 1
)

echo.
echo FFmpeg versiyonu kontrol ediliyor...
ffmpeg -version
if %errorlevel% neq 0 (
    echo UYARI: FFmpeg PATH'te bulunamadı!
    echo Uygulama otomatik tespit yapacak.
)

echo.
echo Proje derleniyor...
call mvnw.cmd clean compile
if %errorlevel% neq 0 (
    echo HATA: Derleme başarısız!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Kamera Test Uygulaması Başlatılıyor...
echo ========================================
echo.
echo Test Adımları:
echo 1. Canlı Kamera sekmesine tıklayın
echo 2. Kameraları Yenile butonuna tıklayın
echo 3. Listeden bir kamera seçin
echo 4. Önizleme Başlat butonuna tıklayın
echo 5. Kamera görüntüsünün gelip gelmediğini kontrol edin
echo.
echo Uygulama kapatıldığında bu script de sonlanacak.
echo.

call mvnw.cmd javafx:run

echo.
echo Test tamamlandı.
pause
