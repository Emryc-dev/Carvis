# Deploy CARVIS FastAPI on Railway

The Android application, Supabase PostgreSQL/Auth/Storage and Upstash Redis remain unchanged. Railway hosts only the FastAPI service.

## 1. Create the Railway service

1. Push this repository to GitHub.
2. In Railway, choose **New Project → Deploy from GitHub repo**.
3. Select the CARVIS repository.
4. Open the created service and set **Root Directory** to:

   ```text
   /carvision-backend
   ```

5. Under **Config file path**, use:

   ```text
   /carvision-backend/railway.json
   ```

Railpack will install Python 3.11 and `pip install .`. Before each release, Railway runs `alembic upgrade head`. The web process binds to Railway's injected `$PORT`.

## 2. Add environment variables

Copy the real values from the existing backend host or local `carvision-backend/.env` into Railway's **Variables** tab. Do not paste `.env` into Git.

Required variables:

```text
APP_ENV=production
DEBUG=false
API_V1_PREFIX=/api/v1
DATABASE_URL=<Supabase transaction pooler URL, port 6543>
DIRECT_URL=<Supabase direct database URL, port 5432>
SUPABASE_URL=<public Supabase project URL>
SUPABASE_PUBLISHABLE_KEY=<public/publishable key>
SUPABASE_SECRET_KEY=<server-only secret key>
SUPABASE_JWT_AUDIENCE=authenticated
SUPABASE_STORAGE_BUCKET=vehicle-scans
UPSTASH_REDIS_REST_URL=<Upstash REST URL>
UPSTASH_REDIS_REST_TOKEN=<Upstash token>
GEMINI_API_KEY=<Gemini key>
GEMINI_MODEL=gemini-2.5-flash
CORS_ORIGINS=<comma-separated admin web origins>
MAX_IMAGE_BYTES=10485760
MIN_IMAGE_WIDTH=224
MIN_IMAGE_HEIGHT=224
MAX_IMAGE_DIMENSION=12000
AI_TIMEOUT_SECONDS=45
```

`GOOGLE_WEB_CLIENT_ID` may also be stored on Railway for configuration parity, but Google sign-in is exchanged by Android through Supabase and the FastAPI service does not currently consume this variable.

Do not create Railway PostgreSQL or Redis services. CARVIS continues to use Supabase and Upstash.

## 3. Generate a public domain

After the first successful deployment:

1. Open **Settings → Networking**.
2. Select **Generate Domain**.
3. Verify:

   ```text
   https://YOUR-SERVICE.up.railway.app/health
   https://YOUR-SERVICE.up.railway.app/ready
   https://YOUR-SERVICE.up.railway.app/docs
   ```

`/health` must return `{"status":"ok"}`. `/ready` must return `"status":"ready"`; otherwise inspect the dependency flags without exposing secret values.

## 4. Point Android to Railway

Set this in the local, ignored `carvision-backend/.env`:

```text
API_BASE_URL=https://YOUR-SERVICE.up.railway.app/api/v1
```

Then rebuild the APK. The backend URL is compiled into Android, so an APK already generated for Render will continue calling Render.

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat clean assembleDebug
```

The output is `app/build/outputs/apk/debug/carvis-debug.apk`.

## 5. Cutover checklist

- Confirm Alembic reports revision `0002` or newer in the Railway deployment logs.
- Test `/health`, `/ready` and `/docs` on the Railway domain.
- Sign in from a newly rebuilt APK.
- Import one disposable vehicle image and verify the scan result.
- Add it to Garage, refresh Garage, then delete it.
- Verify Google sign-in from the APK signed with the SHA registered in Google Cloud.
- Keep Render online until the Railway APK passes these checks, then disable Render to avoid confusion.

## Troubleshooting

- **Application failed to respond:** confirm the start command uses `--host 0.0.0.0 --port $PORT`.
- **Migration cannot connect:** verify `DIRECT_URL` and URL-encode the database password.
- **Database errors containing `pgbouncer`:** remove unsupported query parameters from the asyncpg URL; use the Supabase pooler hostname and port directly.
- **`/ready` says configuration required:** add the missing Railway variable indicated by the response.
- **Android still calls Render:** update local `API_BASE_URL` and rebuild/reinstall the APK.
