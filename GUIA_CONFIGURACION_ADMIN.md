# CrysisShot Plugin - Guía de Configuración para Administradores

## Tabla de Contenidos
1. [Configuración Inicial del Plugin](#configuración-inicial-del-plugin)
2. [Archivos de Configuración](#archivos-de-configuración)
3. [Guía de Configuración de Arena](#guía-de-configuración-de-arena)
4. [Configuración de Temas](#configuración-de-temas)
5. [Referencia de Comandos](#referencia-de-comandos)
6. [Solución de Problemas](#solución-de-problemas)

## Configuración Inicial del Plugin

### Requisitos Previos
- Servidor de Minecraft ejecutando Paper/Spigot 1.20+
- Permisos de administrador/operador
- Permisos de WorldEdit (recomendado)

### Pasos de Instalación

1. **Descargar e Instalar**
   ```
   1. Coloca CrysisShot.jar en tu carpeta /plugins/
   2. Reinicia el servidor
   3. El plugin generará los archivos de configuración por defecto
   ```

2. **Configuración Inicial**
   ```
   /cs admin reload
   ```

3. **Verificar Instalación**
   ```
   /cs help
   ```

## Archivos de Configuración

### Configuración Principal (`config.yml`)

Ubicado en `/plugins/CrysisShot/config.yml`

```yaml
# Configuración del Juego
game:
  target-score: 20              # Puntos necesarios para ganar
  min-players: 4                # Jugadores mínimos para comenzar
  max-players: 16               # Jugadores máximos por partida
  respawn-delay: 3              # Segundos antes del respawn
  starting-arrows: 1            # Flechas con las que inicia cada jugador
  combo-thresholds: [3, 5, 7]  # Umbrales de racha de muertes para bonos
  combo-multipliers: [2, 3, 5] # Multiplicadores de puntuación para rachas
  default-arena-world: "world"  # Mundo por defecto para arenas

# Localización
locale:
  default-language: "es"        # Idioma por defecto
  supported-languages: ["en", "es", "fr"]

# Depuración
debug:
  enabled: false                # Activar mensajes de depuración
```

### Configuración de Arenas (`arenas.yml`)

Ubicado en `/plugins/CrysisShot/arenas.yml`

```yaml
arenas:
  arena1:
    name: "Arena del Desierto"
    world: "mundo_arena"
    theme: "DESERT"
    min-players: 4
    max-players: 12
    spawn-points:
      - world: "mundo_arena"
        x: 100.5
        y: 64.0
        z: 200.5
        yaw: 0.0
        pitch: 0.0
      # Agregar más puntos de spawn...
    bounds:
      min:
        x: 50
        y: 60
        z: 150
      max:
        x: 150
        y: 80
        z: 250
    enabled: true
```

## Guía de Configuración de Arena

### Paso 1: Crear Mundo de Arena

1. **Crear un mundo nuevo** (recomendado):
   ```
   /mv create mundo_arena normal
   ```

2. **O usar mundo existente**:
   - Anota el nombre del mundo para la configuración

### Paso 2: Construir Estructura de Arena

1. **Requisitos de Diseño**:
   - Área abierta para combate con arco (mínimo 50x50 bloques)
   - Espacio libre en altura (se recomiendan 20+ bloques)
   - Cobertura natural o construida/obstáculos
   - Sin agua/lava en el área principal de combate

2. **Características Recomendadas para la Arena**:
   - Múltiples niveles/plataformas
   - Puntos de cobertura estratégicos
   - Líneas de visión claras
   - Separación de puntos de spawn

### Paso 3: Configurar Arena Usando Comandos

#### Creación Básica de Arena

1. **Iniciar Configuración de Arena**:
   ```
   /cs admin arena create <nombre-arena>
   ```

2. **Establecer Mundo de Arena**:
   ```
   /cs admin arena set-world <nombre-arena> <nombre-mundo>
   ```

3. **Establecer Tema de Arena**:
   ```
   /cs admin arena set-theme <nombre-arena> <tema>
   ```
   Temas disponibles: `MEDIEVAL`, `FUTURISTIC`, `DESERT`, `FOREST`, `NETHER`, `END`

#### Configurar Puntos de Spawn

1. **Agregar Puntos de Spawn** (párate en la ubicación deseada):
   ```
   /cs admin arena add-spawn <nombre-arena>
   ```
   - Agrega 8-16 puntos de spawn alrededor de la arena
   - Asegura un posicionamiento equilibrado
   - Orienta los puntos de spawn hacia el centro

2. **Listar Spawns Actuales**:
   ```
   /cs admin arena list-spawns <nombre-arena>
   ```

3. **Eliminar Punto de Spawn**:
   ```
   /cs admin arena remove-spawn <nombre-arena> <índice>
   ```

#### Establecer Límites de Arena

1. **Método 1: Selección de WorldEdit**:
   ```
   // Selecciona área con la varita de WorldEdit
   //pos1 y //pos2
   /cs admin arena set-bounds <nombre-arena>
   ```

2. **Método 2: Coordenadas Manuales**:
   ```
   /cs admin arena set-bounds <nombre-arena> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>
   ```

#### Configurar Ajustes de Arena

1. **Establecer Límites de Jugadores**:
   ```
   /cs admin arena set-min-players <nombre-arena> <número>
   /cs admin arena set-max-players <nombre-arena> <número>
   ```

2. **Habilitar Arena**:
   ```
   /cs admin arena enable <nombre-arena>
   ```

### Paso 4: Probar Arena

1. **Probar Configuración de Arena**:
   ```
   /cs admin arena test <nombre-arena>
   ```

2. **Unirse a Arena para Pruebas**:
   ```
   /cs join <nombre-arena>
   ```

3. **Verificar Estado de Arena**:
   ```
   /cs admin arena info <nombre-arena>
   ```

## Configuración de Temas

### Temas Disponibles

| Tema | Descripción | Efectos Especiales |
|------|-------------|-------------------|
| `MEDIEVAL` | Ambientación de castillo/medieval | Partículas de piedra, iluminación con antorchas |
| `FUTURISTIC` | Ambiente de ciencia ficción | Partículas de ender, sonidos tecnológicos |
| `DESERT` | Dunas de arena y oasis | Partículas de arena, efectos de calor |
| `FOREST` | Ambientación boscosa | Partículas de hojas, sonidos de la naturaleza |
| `NETHER` | Paisaje infernal | Partículas de fuego, efectos de lava |
| `END` | Estilo de dimensión End | Partículas de vacío, sonidos etéreos |

### Comandos de Temas

1. **Previsualizar Efectos de Tema**:
   ```
   /cs admin theme preview <nombre-tema>
   ```

2. **Obtener Pautas de Tema**:
   ```
   /cs admin theme guidelines <nombre-tema>
   ```

3. **Aplicar Efectos de Tema**:
   ```
   /cs admin theme effects <nombre-arena>
   ```

### Configuración de Tema Personalizado

Edita el tema de arena en `arenas.yml`:

```yaml
arenas:
  arena_personalizada:
    theme: "DESERT"
    theme-config:
      particle-effects: true
      ambient-sounds: true
      special-blocks: ["SAND", "SANDSTONE"]
      lighting: "TORCH"
```

## Referencia de Comandos

### Comandos de Administrador

| Comando | Permiso | Descripción |
|---------|---------|-------------|
| `/cs admin reload` | `crysisshot.admin` | Recargar configuración del plugin |
| `/cs admin arena create <nombre>` | `crysisshot.admin` | Crear nueva arena |
| `/cs admin arena delete <nombre>` | `crysisshot.admin` | Eliminar arena |
| `/cs admin arena enable <nombre>` | `crysisshot.admin` | Habilitar arena |
| `/cs admin arena disable <nombre>` | `crysisshot.admin` | Deshabilitar arena |
| `/cs admin arena info <nombre>` | `crysisshot.admin` | Mostrar información de arena |
| `/cs admin arena list` | `crysisshot.admin` | Listar todas las arenas |
| `/cs admin arena test <nombre>` | `crysisshot.admin` | Probar configuración de arena |

### Comandos de Configuración de Arena

| Comando | Descripción |
|---------|-------------|
| `/cs admin arena set-world <arena> <mundo>` | Establecer mundo de arena |
| `/cs admin arena set-theme <arena> <tema>` | Establecer tema de arena |
| `/cs admin arena add-spawn <arena>` | Agregar punto de spawn en ubicación actual |
| `/cs admin arena remove-spawn <arena> <índice>` | Eliminar punto de spawn |
| `/cs admin arena list-spawns <arena>` | Listar puntos de spawn |
| `/cs admin arena set-bounds <arena>` | Establecer límites desde selección de WorldEdit |
| `/cs admin arena set-min-players <arena> <num>` | Establecer jugadores mínimos |
| `/cs admin arena set-max-players <arena> <num>` | Establecer jugadores máximos |

### Comandos de Temas

| Comando | Descripción |
|---------|-------------|
| `/cs admin theme preview <tema>` | Previsualizar efectos de tema |
| `/cs admin theme guidelines <tema>` | Obtener pautas de construcción |
| `/cs admin theme effects <arena>` | Aplicar efectos de tema a arena |

## Solución de Problemas

### Problemas Comunes

1. **El Plugin No Carga**
   - Revisa los logs del servidor en busca de errores
   - Verifica compatibilidad de versión de Java
   - Asegúrate de que todas las dependencias estén presentes

2. **Los Comandos de Arena No Funcionan**
   - Verifica permisos de admin: `crysisshot.admin`
   - Revisa si el nombre de arena contiene caracteres especiales
   - Asegúrate de que el mundo exista y esté cargado

3. **Los Jugadores No Pueden Unirse a la Arena**
   - Verifica si la arena está habilitada: `/cs admin arena info <nombre>`
   - Verifica que los puntos de spawn estén establecidos: `/cs admin arena list-spawns <nombre>`
   - Revisa los límites de cantidad de jugadores

4. **Los Puntos de Spawn No Funcionan**
   - Asegúrate de que los puntos de spawn estén dentro de los límites de arena
   - Verifica el nivel Y (los jugadores aparecen 1 bloque arriba)
   - Verifica que el mundo exista y los chunks estén cargados

### Modo de Depuración

Habilita el modo de depuración en `config.yml`:

```yaml
debug:
  enabled: true
```

Luego recarga: `/cs admin reload`

### Archivos de Log

Revisa estos archivos de log para errores:
- `server.log` - Salida de consola del servidor
- `plugins/CrysisShot/debug.log` - Información de depuración del plugin

### Consejos de Rendimiento

1. **Tamaño de Arena**
   - Mantén las arenas de tamaño razonable (100x100 máximo recomendado)
   - Evita redstone/entidades excesivas en áreas de arena

2. **Puntos de Spawn**
   - Usa 1.5-2x la cantidad de jugadores para puntos de spawn
   - Distribuye uniformemente alrededor de la arena

3. **Gestión de Mundos**
   - Considera mundos separados para arenas
   - Precarga chunks de arena si es posible

### Soporte

No hay.
Ajo y agua.
