Projede verilen bütün mimarisel-teknik kararları ve karar geçmişini içeren dökümantasyondur.

---

### Dependency Injection Kütüphanesi

- Seçim*: **Hilt**

- Son Güncelleme Tarihi*: 02.07.2026

- Alternatifler: **Koin**

- Sebep: **Opsiyonel**


### Navigasyon

- Seçim: **Compose Navigation**

- Son Güncelleme Tarihi: 02.07.2026


### Sunum Katmanı Mimari Deseni

- Seçim: **MVI (Model–View–Intent)**

- Son Güncelleme Tarihi: 03.07.2026

- Alternatifler: **MVVM**

- Sebep: Tek yönlü veri akışı + tek kaynak (single source of truth) state; test edilebilir ve öngörülebilir UI.


### State Yönetimi (Sunum)

- Seçim: **Kotlin StateFlow + collectAsStateWithLifecycle**

- Son Güncelleme Tarihi: 03.07.2026

- Alternatifler: **LiveData**

- Sebep: Compose ile lifecycle-aware, sıcak (hot) state yayını.


### ViewModel Enjeksiyonu

- Seçim: **Hilt + hiltViewModel()** (androidx.hilt:hilt-navigation-compose)

- Son Güncelleme Tarihi: 04.07.2026

- Alternatifler: **Manuel ViewModelProvider.Factory**

- Sebep: Mevcut DI kararı (Hilt) ile tutarlılık.

- Not: androidx.hilt **1.3.0** ile `hiltViewModel()` deprecated-olmayan hâliyle `androidx.hilt.lifecycle.viewmodel.compose` paketinden import edilir; `androidx.hilt.navigation.compose` içindeki kopya kullanımdan kaldırılmıştır. Bağımlılık olarak `hilt-navigation-compose` tutulur (Compose Navigation ile navigasyon-scope'lu VM için) ve bu paket lifecycle-viewmodel-compose'a transitively bağımlıdır.