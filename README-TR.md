# MediaShift - Professional Media Converter

🎬 **FFmpeg tabanlı profesyonel medya dönüştürme uygulaması**

## 🚨 **KURULUM GEREKSINIMLERI**

### Java Kurulumu (Zorunlu)
**Sistemde Java kurulu olmalıdır.** JDK 17 ve üst sürümler için uyumludur.

Java kurulduktan sonra:
1. **Windows tuşu + R** → `sysdm.cpl` → Enter
2. **"Advanced"** sekmesi → **"Environment Variables"**
3. **System variables** bölümünde:

**JAVA_HOME Ekle:**
- **"New"** → Variable name: `JAVA_HOME`
- **Value:** `C:\Program Files\Java\jdk-sürümünüzüburaya yazın` (Java kurulum yolun, program files/java yoluna gidip sürümübulabilirsiniz.)

**PATH'e Ekle:**
- **PATH** değişkenini seç → **"Edit"**
- **"New"** → `%JAVA_HOME%\bin` ekle
- **OK** → **OK** → **OK**

### FFmpeg Kurulumu (Chocolatey ile)

**Chocolatey Kurulumu:**
```cmd

choco --version

```

Sistemde kurulu choco yoksa:

```powershell

Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

```

**FFmpeg Kurulumu:**

```cmd

choco install ffmpeg

```
Lisans onayı isterse **Y** tuşuna basınız.

## 🚀 **KULLANIM**

### 1. Proje Derleme

```cmd

mvnw.cmd clean compile

```

### 2. Uygulamayı Çalıştırma

```cmd

run.bat

```

### 3. Sorun Giderme

```cmd

test-run.bat

```

## 🔧 **SORUN GİDERME**

### Java Hatası

```cmd

java -version

```
Java bulunamazsa: https://www.oracle.com/java/technologies/downloads/

### FFmpeg Hatası

```cmd

ffmpeg -version

```
FFmpeg bulunamazsa: `choco install ffmpeg`

### Uygulama Açılmıyor

1. `test-run.bat` çalıştırın

2. Java ve FFmpeg kurulumunu kontrol edin

3. Windows Defender'ı geçici devre dışı bırakın

## 🌟 **ÖZELLİKLER**

- 🎥 **Video Dönüştürme**: MP4, AVI, MOV, MKV
- 🎵 **Ses Dönüştürme**: MP3, AAC, WAV, FLAC
- ⚡ **Toplu İşlem**: Birden çok dosya
- 🎛️ **Gelişmiş Ayarlar**: Bitrate, çözünürlük, codec
- 📱 **Modern UI**: JavaFX arayüzü
- 🔄 **İlerleme Takibi**: Real-time durum

---

**🎯 Not**: Bu uygulama sistem Java ve FFmpeg kullanır. Manuel kurulum gereklidir.