# UrbanLift Web — Phase 1 (Auth UI)

Production-minded UI for **passenger** (Auth Service) and **driver** (Driver Service) authentication. Built so you can debug each backend independently.

## Stack

- **Vite 5** + **React 18** + **TypeScript**
- **Tailwind CSS** — custom “night + signal amber + aqua” theme, **Syne** / **DM Sans** fonts
- **React Router** — `/`, `/passenger`, `/driver`

## Run

```bash
cd urbanlift-web
npm install
npm run dev
```

Open **http://localhost:5173**.

### Backends

1. **MySQL** running with Auth + Driver schemas  
2. **Auth Service** — `http://localhost:7475`  
3. **Driver Service** — `http://localhost:8081`

The dev server **proxies** (default):

| Prefix      | Target              |
|------------|---------------------|
| `/__auth`  | `http://localhost:7475` |
| `/__driver`| `http://localhost:8081` |

So the browser keeps **httpOnly cookies** (`JWT_TOKEN`, `DRIVER_JWT`) on `localhost:5173` without CORS pain.

**Direct API mode:** copy `.env.example` → `.env`, set `VITE_USE_PROXY=false` and full `VITE_AUTH_API_BASE` / `VITE_DRIVER_API_BASE`. CORS is enabled on Auth & Driver for `http://localhost:5173`.

## Phase 1 scope

- Passenger: signup, signin, validate session  
- Driver: 3-step signup (profile → compliance → vehicle), signin, validate  
- Clear success/error surfaces for API responses  

## Next phases (not in this folder yet)

- Booking flow, maps, driver online, payments — add routes under `src/pages/` when ready.
