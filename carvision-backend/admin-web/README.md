# CarVision Admin Web

React, Vite, Tailwind CSS, shadcn/ui, and the `@efferd/dashboard-5` block adapted to the CarVision FastAPI API.

## Environment

Copy `.env.example` to `.env` and configure:

- `VITE_API_URL`: FastAPI origin, without `/api/v1`.
- `VITE_SUPABASE_URL`: Supabase project URL.
- `VITE_SUPABASE_ANON_KEY`: Supabase publishable/anonymous client key. Never use the secret/service-role key here.

## Administrator authorization

The backend accepts only a valid Supabase user whose trusted `app_metadata` contains either:

```json
{ "role": "admin" }
```

or:

```json
{ "roles": ["admin"] }
```

Set this metadata from a trusted server or the Supabase dashboard. Never allow a browser or mobile client to write `app_metadata`.

## Run

```bash
npm install
npm run dev
```

The application provides working navigation, server-side search, pagination, refresh, theme switching, API documentation access, Google OAuth, email magic-link authentication, record inspection, and sign-out.

## Build

```bash
npm run build
```

The production output is generated in `dist/`.

## Railway deployment

Deploy this directory as a separate Railway service. Follow `RAILWAY_DEPLOYMENT.md`. The production command is `npm start`, which serves the built SPA from `dist/` with client-side routing fallback.
