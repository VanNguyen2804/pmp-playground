# Frontend ↔ Backend connection

## Root cause fixed

The previous Blueprint used `fromService.property: host`. That value is a private-network hostname and cannot be reached by JavaScript running in an end user's browser.

The frontend now receives the backend's public `RENDER_EXTERNAL_URL` and generates:

```json
{
  "apiBaseUrl": "https://pmp-playground-api.onrender.com/api"
}
```

## Manual Render services

When services are not created from `render.yaml`, add this variable to the frontend Static Site:

```text
API_BASE_URL=https://YOUR-BACKEND.onrender.com
```

Then redeploy the frontend with **Clear build cache & deploy**.

On the backend Web Service, set:

```text
CORS_ALLOWED_ORIGINS=https://YOUR-FRONTEND.onrender.com
```

Multiple origins are comma-separated. Optional preview support:

```text
CORS_ALLOWED_ORIGIN_PATTERNS=https://*.onrender.com
```

## Browser verification

Open:

```text
https://YOUR-FRONTEND.onrender.com/app-config.json
```

It must contain a public HTTPS backend URL ending in `/api`, never `localhost` and never a private hostname.

Then open:

```text
https://YOUR-BACKEND.onrender.com/api/categories
```

A JSON response confirms that the backend API is reachable.
