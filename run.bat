@echo off

REM PowerShell'den calistiriliyorsa CMD'ye gecis yap
if defined PSModulePath if "%1" neq "__FROM_CMD__" (
    echo [INFO] PowerShell tespit edildi, CMD'ye geciliyor...
    cmd /c "%~f0" __FROM_CMD__ %*
    exit /b %errorlevel%
)

REM CMD parametresini temizle
if "%1"=="__FROM_CMD__" shift

setlocal enabledelayedexpansion
echo.
echo =================================================================
echo                       MediaShift v1.0.3
echo              Professional Media Converter
echo =================================================================
echo.

REM Java kontrol et
echo [CHECK] Java kontrol ediliyor...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [HATA] Java bulunamadi!
    echo [INFO] Lutfen Java kurun: https://www.oracle.com/java/technologies/downloads/
    echo [INFO] Ya da simple-setup.bat calistirin.
    echo.
    pause
    exit /b 1
)

echo [OK] Java bulundu (sistem)

REM FFmpeg kontrol et
echo [CHECK] FFmpeg kontrol ediliyor...
ffmpeg -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [HATA] FFmpeg bulunamadi!
    echo [INFO] Lutfen FFmpeg kurun: https://ffmpeg.org
    echo [INFO] Ya da simple-setup.bat calistirin.
    echo.
    pause
    exit /b 1
)

echo [OK] FFmpeg bulundu (sistem)

REM Proje derlenmiş mi kontrol et
echo [CHECK] Proje durumu kontrol ediliyor...
if not exist "target\classes" (
    echo [HATA] Proje derlenmemiş!
    echo [INFO] Lutfen once setup.bat dosyasini calistirin.
    echo.
    pause
    exit /b 1
)

echo [OK] Proje hazir: target\classes

echo.
echo [START] Uygulama baslatiliyor...
echo [INFO] Java: Sistem Java
echo [INFO] FFmpeg: Sistem FFmpeg
echo.

REM Uygulamayi baslat
echo [START] JavaFX uygulama baslatiliyor...
call "%~dp0mvnw.cmd" javafx:run

if %errorlevel% neq 0 (
    echo.
    echo [HATA] Uygulama baslatilamadi!
    echo [INFO] Hata kodu: %errorlevel%
    echo [INFO] Ayrintili hata bilgisi yukarida goruntulenmektedir.
    echo.
    echo [TIP] Sorun yasiyorsaniz:
    echo [TIP] 1. setup.bat dosyasini tekrar calistirin
    echo [TIP] 2. Antivirus yaziliminizi kontrol edin
    echo [TIP] 3. Windows Defender'i gecici olarak devre disi birakin
    echo.
    pause
    exit /b 1
)

echo.
echo [EXIT] Uygulama kapatildi.
echo [INFO] Iyi gunler dileriz!
echo.
pause