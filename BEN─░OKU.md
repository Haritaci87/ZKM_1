# Zikirmatik — Android Uygulaması

Uygulama internetsiz çalışır. İnternet yalnızca namaz vakitleri, Ramazan tarihi ve imsakiye için kullanılır. Zikir kayıtların, Kur'an sûreleri ve Esmâ-ül Hüsna telefonun içindedir.

## APK'yı oluşturma

İki yol var. Bilgisayara program kurmak istemiyorsan **1. yolu** seç.

### 1. yol — GitHub ile (kurulum gerektirmez)

1. github.com'da ücretsiz bir hesap aç.
2. Sağ üstten **New repository** seç, bir ad ver (ör. `zikirmatik`) ve **Create repository** de.
3. Açılan sayfada **uploading an existing file** bağlantısına tıkla.
4. Bu klasörün **içindeki her şeyi** (`.github` klasörü dahil) sürükleyip bırak, **Commit changes** de.
   - `.github` klasörü gizli görünebilir. Windows'ta Gezgin > Görünüm > "Gizli öğeler"i aç.
5. Üstteki **Actions** sekmesine geç. "APK oluştur" işi kendiliğinden başlar (5–8 dakika sürer).
   - Başlamazsa: **APK oluştur** > **Run workflow** düğmesine bas.
6. Yeşil tik çıkınca işe tıkla. En alttaki **Artifacts** bölümünden **Zikirmatik-APK** dosyasını indir.
7. İnen zip'i aç, içindeki `app-debug.apk` dosyasını telefona at ve dokunarak kur.
   - Telefon "bilinmeyen kaynak" uyarısı verirse o kaynağa izin ver.

### 2. yol — Android Studio ile

1. Android Studio'yu kur (developer.android.com/studio).
2. **Open** ile bu klasörü aç. İlk açılışta gerekli parçaları kendisi indirir.
3. Menüden **Build > Build App Bundle(s) / APK(s) > Build APK(s)** seç.
4. Bitince çıkan bildirimde **locate**'e tıkla: `app/build/outputs/apk/debug/app-debug.apk`.
5. Telefon USB ile bağlıysa üstteki ▶ (Run) düğmesi uygulamayı doğrudan telefona kurar.

## Eski verilerini uygulamaya taşıma

Tarayıcıdaki sürümle Android uygulaması ayrı yerlerde kayıt tutar. Verilerini taşımak için:

1. Telefonda eski sürümü (zkm.html) aç ve **Yedekle > Dışa Aktar** de. Bir `.json` dosyası iner.
2. Android uygulamasını aç ve **Yedekle > İçe Aktar** de.
3. İndirilenler klasöründen o dosyayı seç.

## Geri tuşu

- Sûre okuyucu, zikir sayacı, zikir ekleme ve yedek penceresi geri tuşuyla kapanır.
- Hiçbir pencere açık değilken geri tuşuna **iki kez** basınca uygulamadan çıkılır.

## Notlar

- Konum izni yalnızca namaz vakitleri için istenir.
- Sayaç ekranı açıkken telefon ekranı kararmaz.
- Vakit hatırlatıcı bildirimi bu sürümde yoktur. Tarayıcı bildirimleri uygulama içinde çalışmadığı için o ayar gizlendi.
