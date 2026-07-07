const CACHE_NAME = "gym-routine-generator-v2";
const CORE_ASSETS = [
  "../index.html",
  "./styles.css",
  "./manifest.webmanifest",
  "./icon.svg"
];
const SCRIPT_ASSETS = [
  "../main.js",
  "../target/scala-3.8.4/gym-tracker-pwa-fastopt/main.js"
];

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache =>
      cache.addAll(CORE_ASSETS).then(() =>
        Promise.all(SCRIPT_ASSETS.map(asset => cache.add(asset).catch(() => undefined)))
      )
    )
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key)))
    )
  );
});

self.addEventListener("fetch", event => {
  event.respondWith(
    caches.match(event.request).then(cached =>
      cached || fetch(event.request).then(response => {
        const copy = response.clone();
        caches.open(CACHE_NAME).then(cache => cache.put(event.request, copy));
        return response;
      })
    )
  );
});
