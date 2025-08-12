@echo off
echo ===================================
echo    MediaShift Test Runner
echo ===================================
echo.

echo [INFO] Java kontrol ediliyor...
java -version
if %errorlevel% neq 0 (
    echo [HATA] Java bulunamadi!
    pause
    exit /b 1
)
echo.

echo [INFO] Proje dosyalari kontrol ediliyor...
if not exist "target\classes" (
    echo [HATA] Proje derlenmemiş! Once 'mvnw clean compile' calistirin.
    pause
    exit /b 1
)
echo [OK] Derleme dosyalari bulundu.
echo.

echo [INFO] Method 1: Maven JavaFX plugin...
call mvnw.cmd javafx:run
if %errorlevel% equ 0 (
    echo [SUCCESS] Uygulama Method 1 ile calisti!
    goto END
)
echo [WARNING] Method 1 basarisiz, Method 2 deneniyor...
echo.

echo [INFO] Method 2: Direct Java execution...
java -cp "target\classes;target\lib\*" com.ffmpeg.gui.Main
if %errorlevel% equ 0 (
    echo [SUCCESS] Uygulama Method 2 ile calisti!
    goto END
)
echo [WARNING] Method 2 basarisiz, Method 3 deneniyor...
echo.

echo [INFO] Method 3: JavaFX modules...
java --module-path "target\lib" --add-modules javafx.controls,javafx.media,javafx.fxml -cp "target\classes" com.ffmpeg.gui.Main
if %errorlevel% equ 0 (
    echo [SUCCESS] Uygulama Method 3 ile calisti!
    goto END
)

echo [ERROR] Tum methodlar basarisiz!
echo [INFO] Detayli hata bilgisi icin:
echo [INFO] java -cp "target\classes" com.ffmpeg.gui.Main
echo.

:END
echo.
echo Test tamamlandi.
pause
