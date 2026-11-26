# Block Blast AI Çözücü - Android APK Paketi

Bu paket, uygulamanızı doğrudan Android telefonunuza **.apk** olarak kurabilmeniz için hazırlanmış tam proje dosyalarını içerir.

## Seçenek 1: Bilgisayarsız / Tek Tıkla GitHub Actions ile APK Oluşturma (En Kolay)
1. GitHub'da ücretsiz yeni bir depo (repository) açın ve bu ZIP içindeki tüm dosyaları yükleyin.
2. GitHub deposunda **Actions** sekmesine girin ve **Build Android APK** iş akışını çalıştırın (**Run workflow**).
3. ~2 dakika içinde oluşan **BlockBlastAI-debug-apk** dosyasını telefonunuza indirip doğrudan kurun.

## Seçenek 2: Bilgisayarda Android Studio / Terminal ile APK Derleme
```bash
npm install
npx cap add android
npx cap sync android
cd android
./gradlew assembleDebug
```
Oluşan APK dosyası şu konumda yer alır:
`android/app/build/outputs/apk/debug/app-debug.apk`
