const CACHE_NAME = 'nexus-static-v3';

// Política estática: solo recursos públicos sin información financiera ni de sesión.
const STATIC_ALLOWLIST = [
    '/manifest.webmanifest',
    '/offline.html',
    '/css/nexus-tokens.css',
    '/css/nexus-shell.css',
    '/css/nexus-components.css',
    '/css/nexus-views.css',
    '/css/nexus.css',
    '/js/nexus-shell.js',
    '/js/pwa-register.js',
    '/img/nexus-isotipo.svg',
    '/img/icon-192.png',
    '/img/icon-512.png'
];

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => cache.addAll(STATIC_ALLOWLIST))
    );
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) =>
            Promise.all(
                keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
            )
        )
    );
    self.clients.claim();
});

self.addEventListener('fetch', (event) => {
    const request = event.request;
    if (request.method !== 'GET') {
        return;
    }

    const url = new URL(request.url);
    if (url.origin !== self.location.origin) {
        return;
    }

    // Nunca cachear vistas HTML ni rutas autenticadas financieras.
    if (request.mode === 'navigate') {
        event.respondWith(
            fetch(request).catch(() => caches.match('/offline.html'))
        );
        return;
    }

    if (STATIC_ALLOWLIST.includes(url.pathname)) {
        event.respondWith(
            caches.match(request).then((cached) =>
                cached || fetch(request).then((response) => {
                    if (response && response.ok) {
                        const clone = response.clone();
                        caches.open(CACHE_NAME).then((cache) => cache.put(request, clone));
                    }
                    return response;
                })
            )
        );
    }
});
