const CACHE_NAME = 'nexus-static-v1';

// Lista explicita y cerrada de recursos estaticos publicos permitidos.
// PROHIBIDO cachear HTML autenticado, /inicio, /grupo/**, /asistente, /integraciones,
// /liquidaciones, respuestas POST, tokens o datos financieros privados.
const STATIC_ALLOWLIST = [
    '/css/partly.css',
    '/css/nexus.css',
    '/img/nexus-isotipo.svg',
    '/img/nexus-logo-horizontal.svg',
    '/img/icon-192.png',
    '/img/icon-512.png',
    '/js/pwa-register.js',
    '/offline.html',
    '/manifest.webmanifest'
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
            Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
        )
    );
    self.clients.claim();
});

self.addEventListener('fetch', (event) => {
    const req = event.request;
    if (req.method !== 'GET') {
        return;
    }

    const url = new URL(req.url);
    if (url.origin !== self.location.origin) {
        return;
    }

    if (STATIC_ALLOWLIST.includes(url.pathname)) {
        event.respondWith(
            caches.match(req).then((cached) => cached || fetch(req))
        );
        return;
    }

    // Todas las rutas financieras y HTML autenticado dependen estrictamente de la red.
    if (req.mode === 'navigate') {
        event.respondWith(
            fetch(req).catch(() => caches.match('/offline.html'))
        );
    }
});
