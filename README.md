# Lab #5 – BluePrints en Tiempo Real (STOMP + WebSocket)
## Escuela Colombiana de Ingeniería – Arquitecturas de Software
**Estudiante:** Brayan Loaiza  
**Backend:** https://github.com/brloa05/lab5_backend_realTime_BluePrints_Brayan_Loaiza  
**Frontend:** https://github.com/brloa05/Lab_P4_BluePrints_RealTime-Sokets_brayanLoaiza

---

## Video de demostración

https://github.com/brloa05/lab5_backend_realTime_BluePrints_Brayan_Loaiza/blob/main/docs/demo.mp4

---

## Objetivo

Extender la API REST de blueprints (Lab #4) con soporte de **colaboración en tiempo real** usando **STOMP sobre WebSocket** (Spring Boot), permitiendo que múltiples clientes dibujen el mismo plano de forma simultánea.

---

## Arquitectura

```
React (Vite) — Frontend
 │
 ├── HTTP REST  ──────────────────────> Spring Boot :8080
 │    GET /api/blueprints/{a}/{n}        (estado inicial del plano)
 │    POST/PUT/DELETE /api/blueprints    (CRUD)
 │
 └── WebSocket / STOMP ───────────────> Spring Boot :8080/ws-blueprints
      Publica:   /app/draw              (envía nuevo punto)
      Suscribe:  /topic/blueprints.{author}.{name}  (recibe actualizaciones)
```

```
src/main/java/edu/eci/arsw/blueprints
  ├── model/        → Blueprint, Point, DrawEvent, BlueprintUpdate
  ├── persistence/  → BlueprintPersistence
  │    ├── InMemoryBlueprintPersistence  (perfil: !postgres)
  │    ├── PostgresBlueprintPersistence  (perfil: postgres)
  │    └── BlueprintJpaRepository
  ├── services/     → BlueprintsServices
  ├── filters/      → IdentityFilter / RedundancyFilter / UndersamplingFilter
  ├── controllers/  → BlueprintsAPIController (REST)
  │                   StompBlueprintController (WebSocket)
  └── config/       → WebSocketConfig, OpenApiConfig
```

---

## Requisitos previos

- Java 21
- Maven 3.9+
- Node.js 18+ (para el frontend)
- Docker Desktop (opcional — solo para PostgreSQL)

---

## Puesta en marcha

### 1. Backend

**Con persistencia en memoria (por defecto):**
```bash
mvn spring-boot:run
```

**Con persistencia en PostgreSQL:**
```bash
docker compose up -d
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

### 2. Frontend

```bash
cd Lab_P4_BluePrints_RealTime_brayanLoaiza
npm install
npm run dev
# http://localhost:5173
```

### 3. Probar colaboración

1. Abrir `http://localhost:5173` en dos pestañas
2. En ambas escribir el mismo `autor` y `plano` (ej: `john` / `house`)
3. Esperar ~600ms para que se conecte automáticamente
4. Dibujar en una pestaña — los trazos aparecen en la otra en tiempo real

---

## API REST

Ruta base: `/api/blueprints`  
CORS habilitado para todos los orígenes en desarrollo.

| Método | Ruta | Descripción | Código |
|--------|------|-------------|--------|
| GET | `/api/blueprints` | Todos los blueprints | 200 |
| GET | `/api/blueprints?author={a}` | Blueprints por autor | 200, 404 |
| GET | `/api/blueprints/{author}/{name}` | Blueprint específico | 200, 404 |
| POST | `/api/blueprints` | Crear blueprint | 201, 409 |
| PUT | `/api/blueprints/{author}/{name}` | Reemplazar puntos | 200, 404 |
| DELETE | `/api/blueprints/{author}/{name}` | Eliminar blueprint | 204, 404 |

### Ejemplos (PowerShell)

```powershell
# Obtener blueprint
Invoke-RestMethod http://localhost:8080/api/blueprints/john/house

# Crear blueprint
Invoke-RestMethod http://localhost:8080/api/blueprints `
  -Method Post -ContentType 'application/json' `
  -Body '{"author":"john","name":"test","points":[{"x":0,"y":0}]}'

# Reemplazar puntos
Invoke-RestMethod http://localhost:8080/api/blueprints/john/test `
  -Method Put -ContentType 'application/json' `
  -Body '{"points":[{"x":10,"y":10},{"x":50,"y":50}]}'

# Eliminar blueprint
Invoke-RestMethod http://localhost:8080/api/blueprints/john/test -Method Delete
```

---

## Protocolo STOMP (Tiempo Real)

**Endpoint WebSocket:** `ws://localhost:8080/ws-blueprints`

### Publicar un punto (cliente → servidor)

**Destino:** `/app/draw`  
**Body:**
```json
{ "author": "john", "name": "house", "point": { "x": 120, "y": 80 } }
```

### Recibir actualización (servidor → clientes)

**Topic:** `/topic/blueprints.{author}.{name}`  
**Body:**
```json
{ "author": "john", "name": "house", "points": [{"x":0,"y":0}, {"x":120,"y":80}] }
```

> El servidor hace broadcast de la **lista completa** de puntos acumulados en cada evento, no solo el punto nuevo. Esto garantiza que todos los clientes tengan el mismo estado sin importar cuándo se conectaron.

### Flujo completo

```
Cliente A                    Servidor                    Cliente B
   │                            │                            │
   │── GET /api/blueprints ────>│                            │
   │<── { points: [...] } ──────│                            │
   │                            │<── subscribe /topic/... ──│
   │── subscribe /topic/... ───>│                            │
   │                            │                            │
   │── publish /app/draw ──────>│                            │
   │                            │── addPoint() ─────────────│
   │                            │── broadcast /topic/... ──>│
   │<── blueprint-update ───────│                            │
   │                            │                            │
```

---

## Datos de prueba (en memoria)

Al arrancar sin perfil `postgres`, la app carga estos blueprints automáticamente:

| Autor | Plano   | Puntos |
|-------|---------|--------|
| john  | house   | 4      |
| john  | garage  | 3      |
| jane  | garden  | 3      |

---

## Configuración PostgreSQL (opcional)

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: blueprints
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
```

---

## Decisiones técnicas

| Decisión | Alternativa descartada | Razón |
|----------|------------------------|-------|
| STOMP sobre WebSocket nativo | Socket.IO (Node.js) | Spring Boot ya incluye soporte nativo; evita un servidor Node separado |
| Broadcast de lista completa | Broadcast solo del punto nuevo | Garantiza consistencia para clientes que se conectan tarde |
| Debounce 600ms en frontend | Botón "Conectar" | Experiencia más fluida sin UI adicional |
| Perfil `!postgres` para in-memory | Siempre requirir BD | Tests y demos funcionan sin Docker |
