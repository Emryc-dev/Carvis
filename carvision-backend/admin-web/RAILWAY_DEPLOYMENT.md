# Deploy the CARVIS admin dashboard on Railway

Deploy this dashboard as a second Railway service. Do not combine it with the FastAPI process.

## Create the service

1. In the same Railway project, select **New → GitHub Repo**.
2. Select the same CARVIS repository.
3. Name the service `carvision-admin`.
4. Set **Root Directory** to:

   ```text
   /carvision-backend/admin-web
   ```

5. Set **Config file path** to:

   ```text
   /carvision-backend/admin-web/railway.json
   ```

The service runs `npm run build`, then serves the generated SPA with `serve -s dist -l $PORT`.

## Railway variables

Add these variables before deploying:

```text
VITE_API_URL=https://YOUR-BACKEND.up.railway.app
VITE_SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
VITE_SUPABASE_ANON_KEY=YOUR_SUPABASE_PUBLISHABLE_KEY
```

`VITE_API_URL` must be the backend origin without `/api/v1`. The dashboard code appends `/api/v1/admin/...` itself.

Never add `SUPABASE_SECRET_KEY`, a service-role key, the database URL, Gemini key or Upstash token to the admin service. Every `VITE_` variable is public in the browser bundle.

## Public domain and cross-origin settings

Generate a public Railway domain for the admin service. Assume it is:

```text
https://carvision-admin.up.railway.app
```

Then update the backend Railway variable:

```text
CORS_ORIGINS=https://carvision-admin.up.railway.app
```

If multiple web origins are needed, separate them with commas.

In Supabase, open **Authentication → URL Configuration** and add the admin origin to **Redirect URLs**. Set the appropriate production Site URL or provide the admin URL as an allowed redirect so magic links and Google OAuth can return to the dashboard.

For the Google OAuth Web client, add this under **Authorized JavaScript origins**:

```text
https://carvision-admin.up.railway.app
```

Keep the Supabase callback under **Authorized redirect URIs**:

```text
https://YOUR_PROJECT_REF.supabase.co/auth/v1/callback
```

## Verification

1. Open the admin Railway domain.
2. Sign in using the Supabase administrator account.
3. Confirm the account has trusted `app_metadata.role = "admin"`.
4. Verify Overview, Users, Vehicles, Scans and AI requests load from the Railway backend.
5. Test search, pagination, refresh and sign-out.

If the page loads but API calls fail, inspect browser DevTools:

- CORS error: update backend `CORS_ORIGINS` and redeploy the backend.
- `401`: the Supabase session is missing or expired.
- `403 admin_required`: the account is authenticated but lacks the trusted admin role.
- Old API URL: Railway baked an earlier `VITE_API_URL` into the build; redeploy the admin service.
