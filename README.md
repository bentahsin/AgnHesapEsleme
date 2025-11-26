![Agn](https://cdn.modrinth.com/data/cached_images/17f1bf40e839c4c357808e39cc7c7844877a365e_0.webp)

## Gereksinimler

- Java 8 veya daha yeni bir sürüm
- Spigot veya Forkleri (Paper, Purpur gibi)
- LuckPerms


## **ÖZELLİKLER:** (TR)

- **Otomatik Bilgilendirme Sistemi:** Eklenti, Minecraft sunucusunda gerçekleşen belirli olayları otomatik olarak Discord sunucusuna bildirir. Bu sistem sayesinde oyuncu aktiviteleri, eşleşme durumu ve özel bildirimler Discord üzerinden ilgili kanallara gönderilir. Bildirimler, config.yml dosyası aracılığıyla özelleştirilebilir ve kanal bazlı yönlendirme desteklenir.


- **2FA(İki Aşamalı Doğrulama Sistemi):** Hesabınıza farklı bir IP adresinden giriş yapıldığında, güvenliğiniz için Discord üzerinden size bir doğrulama mesajı gönderilir. Bu durumda, dilerseniz giriş yapan oyuncuyu sunucudan atabilir ya da doğrulama mesajını onaylayarak devam etmesine izin verebilirsiniz.

- **Dahili Gömülü Discord Botu:** Eklenti, harici bir bot kurulumuna gerek kalmadan kendi içerisinde çalışan bir entegre Discord botu barındırır.
Bu sayede sistem, herhangi bir 3. parti uygulamaya ihtiyaç duymadan bağımsız olarak çalışabilir.
Bot, Discord API üzerinden:
Mesaj gönderme
Rol atama
Kullanıcı adı güncelleme
Onay sistemi yönetimi
gibi işlemleri doğrudan gerçekleştirebilir.

- **Onaylı Kullanıcı Eşleştirme Sistemi:** Kullanıcıların Minecraft hesaplarını Discord hesaplarıyla bağlamadan önce bir onay sürecinden geçmeleri gerekmektedir. Bu sistem, sunucuda yetkisiz kullanıcı eşleşmelerinin önüne geçmek ve güvenliği artırmak için geliştirilmiştir.

- **6 Adet Dil Desteği:**
   - Çince (Chinese)
   - Türkçe (Turkish)
   - İngilizce (English)
   - Fransızca (French)
   - İspanyolca (Spanish)
   - Almanca (German)

- **Kullanıcı Adı Güncelleme Sistemi:** Başarılı eşleştirme sonrasında, kullanıcının Discord üzerindeki görünen adı otomatik olarak Minecraft kullanıcı adı ile güncellenir.
Bu özellik:
Sunucu içi düzeni artırır
Oyuncuların kimlik takibini kolaylaştırır
Anonim kullanıcı adlarının önüne geçer
Yapılandırma dosyası üzerinden bu özellik aktif/pasif hale getirilebilir.

- **Rol Atama ve Yetkilendirme Sistemi:** Eklenti, oyuncu eşleşmesini tamamladıktan sonra kullanıcının sahip olduğu özel üyelik durumuna göre Discord üzerindeki belirli rolleri otomatik olarak atar.
Rol tanımlamaları config.yml üzerinden yapılır ve şu kurallar desteklenir:
VIP, Premium, Elit gibi özel üyelik sistemleri
Çoklu rol desteği (birden fazla rol aynı anda atanabilir)

- **Otomatik Rol Verme:**
Oyuncu Hesabını Eşlediğinde, Configrasyonda belirlediğiniz, Verified-Rol-Id Oyuncuya Discord Üzerinden İletilir.

- **GünlükÖdül Sistemi:**
Discord Bilgilendirme Mesajı Üzerinden, Ödül kontrol Butonuna Basarak Ödüllerinizi Kontrol Edebilir, Her Güne özel ödüller belirleyebilirsiniz, Tamamen Özelleştirilebilir ödüller.

###   Kurulum Talimatları

Yayınlanan jar dosyasını plugins/ klasörüne kopyalayın.
Sunucuyu başlatın ve plugins/AgnHesapEsle/ altında config.yml oluşturulmasına izin verin.
Gerekirse config.yml dosyasını düzenleyin.

### Komutlar

- **/hesapeşle eşle**: ➤ Discord botuna gönderilecek olan eşleştirme kodunu görüntüler.
- **/hesapeşle kaldır**: ➤ Mevcut eşleştirme bağlantısını kaldırı
- **/hesapeşle yenile**: ➤ messages.yml dosyasını yeniden yükler.
- **/hesapeşle kodiptal**: ➤ Onay sürecindeki kodunuzu iptal eder.
- **/hesapeşle liste** ➤ Hesabını eşleyen oyuncuların listesini gösterir.
- **/hesapeşle sıfırla OyuncuAdı** ➤ Belirtilen oyuncunun eşleşme bağlantısını sıfırlar.

### PLACEHOLDER
- **%agnesesle_server_toplam_eslesme%**: Sunucudaki, Toplam Eşleşme Sayısını Gösterir
- **%agnesesle_durum%**: Oyuncunun Eşledi mi, Eşlemedi mi Durumunu Gösterir.
- **%agnesesle_discord_id%**: Kullanıcının Discord Id'sini gösterir.
- **%agnesesle_discord_adi%**: Oyuncunun Discord Üzerinde'ki Adını Gösterir.
- **%agnesesle_2fa_durum%**: Oyuncunun 2FA(İki Aşamalı Doğrulama) Durumunu gösterir.


### İzinler

```
agnesesle.admin (Tüm Komutlara Erişim Sağlar.)
```
### Veritabanı
- Veri Tabana Dosya .yml Üzeridir İleri Zamanlarda Geçiş Yapılacaktır.

### Yapılandırma:

<div align="center">
<details>
<summary>config.yml Örneği</summary>
<a href="https://github.com/Agnesm2131/AgnHesapEsleme/blob/main/src/main/resources/config.yml" target="_blank" rel="noopener noreferrer">Tıkla</a>
</details>
</div>

<div align="center">
<details>
<summary>messages_tr.yml Dosyası</summary>
<a href="https://github.com/Agnesm2131/AgnHesapEsleme/blob/main/src/main/resources/langs/messages_tr.yml">Tıkla</a>
</details>
</div>

### İletişim


<details>
<summary>İletişim</summary>

Discord: **agnes9s_**
Discord: **bentahsin**

</details>



### ENG

## Requirements
- Java 8 or a newer Version
- Spigot or its forks (like Paper, Purpur) 
- LuckPerms

### FEATURES: (EN)

- **Automatic Notification System:**  
  The plugin automatically reports specific events happening on the Minecraft server to the Discord server. Through this system, player activities, match status, and special notifications are sent to relevant channels on Discord. Notifications can be customized via the `config.yml` file and support channel-based routing.

- **2FA (Two-Factor Authentication System):**  
  When someone logs into your account from a different IP address, a verification message is sent to you via Discord for security purposes. You can either kick the player from the server or approve the login via the verification message.

- **Built-in Embedded Discord Bot:**  
  The plugin includes a built-in integrated Discord bot, eliminating the need for an external bot setup.  
  This allows the system to work independently without any third-party applications.  
  The bot can directly perform operations such as:
  - Sending messages  
  - Assigning roles  
  - Updating usernames  
  - Managing the verification system

- **Verified User Matching System:**  
  Before users can link their Minecraft accounts to their Discord accounts, they must go through a verification process. This system is designed to prevent unauthorized user matches and enhance security on the server.

- **Support for 6 Languages:**
   - Chinese  
   - Turkish  
   - English  
   - French  
   - Spanish  
   - German

- **Username Update System:**  
  After a successful account match, the user’s visible name on Discord is automatically updated to match their Minecraft username.  
  This feature:
    - Improves in-server order  
    - Simplifies player identification  
    - Prevents anonymous usernames  
  It can be enabled/disabled via the configuration file.

- **Role Assignment and Authorization System:**  
  After matching, the plugin automatically assigns specific roles on Discord based on the player's membership status (e.g., VIP, Premium, Elite).  
  Role definitions are managed via `config.yml` and support:
    - Special membership roles  
    - Multi-role assignment

 ## Installation Instructions

 1. Copy the released `.jar` file into the `plugins/` folder.  
2. Start the server and allow the plugin to generate the `config.yml` under `plugins/AgnHesapEsle/`.  
3. Edit the `config.yml` file if necessary.

## Commands

- **/hesapeşle eşle**: ➤ Displays the match code to be sent to the Discord bot.  
- **/hesapeşle kaldır**: ➤ Removes the current match link.  
- **/hesapeşle yenile**: ➤ Reloads the `messages.yml` file.  
- **/hesapeşle kodiptal**: ➤ Cancels the pending verification code.  
- **/hesapeşle liste** ➤ Displays the list of players who have matched their accounts.  
- **/hesapeşle sıfırla <PlayerName>** ➤ Resets the match link of the specified player.

## PLACEHOLDER
- **%agnesesle_server_toplam_eslesme%**: Displays the total number of matches on the server.
- **%agnesesle_durum%**: Shows whether the player has matched or not.
- **%agnesesle_discord_id%**: Displays the user's Discord ID.
- **%agnesesle_discord_adi%**: Shows the player's name on Discord.
- **%agnesesle_2fa_durum%**: Displays the player's 2FA (Two-Factor Authentication) status.

## Contact

<details>
<summary>Contact</summary>

Discord: **agnes9s_**
Discord: **bentahsin**

</details>



## Permissions
- agnesesle.admin (Grants access to all commands.)

## Database
- Data is stored in `.yml` files. Migration to a different system may be implemented in the future.

## Configuration

<div align="center">
<details>
<summary>config.yml Example</summary>
<a href="https://github.com/Agnesm2131/AgnHesapEsleme/blob/main/src/main/resources/config.yml" target="_blank" rel="noopener noreferrer">Click</a>
</details>
</div>


<div align="center">
<details>
<summary>messages_en.yml File</summary>
<a href="https://github.com/Agnesm2131/AgnHesapEsleme/blob/main/src/main/resources/langs/messages_en.yml">Click</a>
</details>
</div>
