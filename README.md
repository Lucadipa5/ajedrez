# Proyecto Ajedrez — Ingeniería de Software 2026

Ajedrez completo en Java con arquitectura hexagonal: un **núcleo** de reglas puro (sin ninguna dependencia
de interfaz ni infraestructura, probado con tests unitarios) y **adaptadores** intercambiables alrededor
(ventana Swing, persistencia en archivo, motor de IA en Python, registros por consola).

## Cómo correrlo

| Qué | Cómo |
|---|---|
| Jugar | `compilar_y_ejecutar.bat` (compila, instala `python-chess` si falta y abre el juego) |
| Tests del núcleo | `correr_tests.bat` (146 tests, sin ninguna infraestructura) |
| Requisitos | Java 17+. Opcional: Python 3 + `pip install -r ia/requirements.txt` para el modo contra la IA |

## Qué hace

- **Reglas básicas**: tablero de 8x8, las 6 piezas con su movimiento, turnos, capturas,
  validación de jugadas ilegales y detección de jaque.
- **Reglas completas**: jaque mate, enroque, captura al paso, promoción, tablas (ahogado,
  material insuficiente, 50 movimientos, triple repetición), deshacer/rehacer, oponente con IA en tres
  dificultades e interfaz gráfica temática.
- **Además**: cuentas con contraseña (PBKDF2) y foto de perfil, sistema de experiencia y niveles,
  lista de jugadas en notación algebraica, pantalla completa, y un núcleo que funciona con tableros
  de cualquier tamaño.

## Estructura

```
src/ajedrez/
├── nucleo/                 reglas del juego: no importa nada de fuera de este paquete
│   ├── dominio/            Posicion, Pieza, TipoPieza, Color, Jugada, EventoJugada, notación algebraica
│   ├── estrategias/        Strategy: cómo se mueve cada pieza (peón, caballo, líneas, rey, enroque, compuesto)
│   ├── fabrica/            Factory: FabricaPiezas (registro tipo → estrategia) y DisposicionInicial
│   ├── tablero/            Tablero (tamaño configurable, estado de la posición) y notación FEN
│   ├── comandos/           Command: mover, enrocar, capturar al paso, historial deshacer/rehacer
│   ├── reglas/             DetectorJaque, ValidadorMovimiento, reglas de tablas, Reglamento
│   ├── estados/            State: turno blanco/negro, jaque mate, tablas
│   ├── servicio/           PartidaAjedrez (puerto de entrada, Observer), SesionDePartida (XP + IA)
│   ├── perfil/             cuentas, XP y el puerto RepositorioPerfiles
│   └── ia/                 puerto MotorAjedrez, ConfiguracionIA, registro de decisiones
├── adaptadores/
│   ├── ui/swing/           ventana, menú, tablero dibujado, diálogos (adaptador de entrada)
│   ├── persistencia/       RepositorioPerfilesArchivo (adaptador de salida)
│   ├── ia/                 MotorPython (habla con ia/motor_ia.py) y registro por consola
│   └── registro/           RegistroPartidaConsola (observador de la partida)
└── tests/                  suite de tests unitarios del núcleo (sin framework)
ia/motor_ia.py              el motor de IA (python-chess + búsqueda alfa-beta)
docs/                       diagrama de clases y justificación de decisiones de diseño
```

La única clase que conoce al núcleo y a todos los adaptadores concretos es `Main` (la raíz de
composición): ahí se conecta cada puerto con el adaptador elegido.

## Documentación

- [docs/arquitectura.md](docs/arquitectura.md): diagrama de clases (UML) y cómo se reparten núcleo, puertos y adaptadores.
- [docs/decisiones-de-diseno.md](docs/decisiones-de-diseno.md): cada decisión en formato *qué / por qué / cuándo romperla*.
