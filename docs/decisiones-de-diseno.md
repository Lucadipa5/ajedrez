# Decisiones de diseño — qué / por qué / cuándo romperla

Cada decisión de diseño del proyecto, con la regla que se adoptó, la razón que la sostiene y las
condiciones en las que convendría desviarse de ella. Ninguna es un dogma: todas tienen su excepción consciente.

---

### 1. La pieza TIENE una estrategia; la Reina y el Rey se arman por composición

- **Qué**: `Pieza` recibe por constructor una `EstrategiaMovimiento`. No hay subclases `Torre`, `Alfil`...
  La Reina es `MovimientoCompuesto(ortogonal, diagonal)` y el Rey es `MovimientoCompuesto(paso, enroque)`.
- **Por qué**: combinar capacidades sin jerarquía. Agregar una pieza o una regla de movimiento es una
  clase nueva; el enroque entró como estrategia aparte sin tocar `MovimientoRey`.
- **Cuándo romperla**: `MovimientoOrtogonal` y `MovimientoDiagonal` sí heredan de `MovimientoLinealBase`:
  es una jerarquía de dos niveles, estable, donde solo varía un dato (las direcciones). Ahí la herencia
  es más simple que componer, y la relación "es-un" es genuina.

### 2. `FabricaPiezas` es un registro, no un `switch`

- **Qué**: un `Map<TipoPieza, Supplier<EstrategiaMovimiento>>` con `registrar(tipo, estrategia)`.
- **Por qué**: Open/Closed. Un `switch` por tipo obliga a reabrirlo con cada pieza nueva. Con el registro,
  una variante registra su pieza desde afuera y ninguna clase existente cambia.
- **Cuándo romperla**: si el juego nunca fuera a tener otra pieza que las seis, el `switch` sería más
  corto y obvio. Como el sistema está pensado para admitir piezas nuevas, el punto de extensión vale la indirección.

### 3. El tablero tiene tamaño por constructor y nadie más sabe que son 8 casillas

- **Qué**: `Tablero(filas, columnas)`; `Posicion` no valida límites, los valida el tablero. El peón
  calcula su fila inicial y la de coronación a partir del tablero.
- **Por qué**: cambiar el tamaño del tablero es la extensión más probable para una variante. Si el 8 vive
  en un solo lugar, el cambio es una línea; si está desparramado, es una cacería.
- **Cuándo romperla**: la notación FEN y la UCI son del ajedrez estándar por definición; ahí el 8 es
  legítimo y `NotacionFEN` lo dice rechazando otros tamaños. También la `DisposicionEstandar` exige 8
  columnas, porque la fila de piezas es la del ajedrez clásico.

### 4. Las jugadas especiales son comandos propios, y un decorador lleva la contabilidad

- **Qué**: `MoverPiezaComando`, `EnroqueComando` y `CapturaAlPasoComando` implementan `ComandoDeJugada`;
  `FabricaComandos` decide cuál corresponde; `ComandoJugada` los envuelve y mantiene la casilla al paso
  y el reloj de los 50 movimientos (y los restaura al deshacer).
- **Por qué**: cada comando sabe deshacerse exactamente (el enroque devuelve dos piezas y sus banderas;
  la captura al paso devuelve un peón que no estaba en el destino). La contabilidad común va en un solo
  lugar en vez de repetirse en tres clases: composición (decorador) en vez de una clase base.
- **Cuándo romperla**: si solo existiera el movimiento común, un único comando sin fábrica alcanzaría.
  La fábrica apareció cuando construir el comando pasó a implicar una decisión.

### 5. El validador simula la jugada con el mismo comando que la aplicaría

- **Qué**: para saber si una jugada deja al propio rey en jaque, `ValidadorMovimiento` clona el tablero,
  le pide a `FabricaComandos` el comando y lo ejecuta sobre el clon.
- **Por qué**: una simulación "mover la pieza y mirar" se equivoca con el enroque (la torre también se
  mueve) y con la captura al paso (desaparece un peón de otra casilla, lo que puede descubrir un jaque).
  Usar el comando real garantiza que lo que se valida es lo que se va a jugar.
- **Cuándo romperla**: en un motor de alto rendimiento clonar el tablero por cada jugada candidata sería
  caro y se usaría hacer/deshacer sobre el mismo tablero. Para un juego de escritorio, la claridad gana:
  optimizar sin haber medido un problema es sobre-ingeniería.

### 6. `Reglamento` agrupa las reglas y entra por constructor

- **Qué**: `PartidaAjedrez(Tablero, DisposicionInicial, Reglamento, SelectorPromocion)`. El reglamento
  junta la fábrica de piezas, la de comandos, el validador y la lista de `ReglaDeTablas`.
  `PartidaAjedrez.estandar()` y `Reglamento.estandar()` arman el ajedrez clásico.
- **Por qué**: inyección de dependencias: la partida no hace `new` de sus reglas, así los tests le dan
  un reglamento sin regla de 50 movimientos o una fábrica con otra pieza. Agruparlas evita un constructor
  de siete parámetros, que es síntoma de una clase que hace demasiado.
- **Cuándo romperla**: `HistorialComandos` e `HistorialPosiciones` sí se crean adentro: son estado
  propio de la partida, sin ninguna implementación alternativa posible. Abstraerlos sería crear una
  interfaz con una sola implementación.

### 7. Las condiciones de tablas son una lista de estrategias

- **Qué**: `ReglaDeTablas` con `ReglaCincuentaMovimientos`, `ReglaRepeticionTriple` y
  `ReglaMaterialInsuficiente`. El ahogado no es una regla de la lista: es "no hay jugadas legales y no hay
  jaque", y lo resuelve la partida junto con el mate.
- **Por qué**: cada regla se prueba sola y se agrega o quita sin tocar la partida (Open/Closed).
  Las tablas por 50 movimientos y repetición se declaran automáticamente, aunque en el reglamento oficial
  sean reclamables: en un juego de escritorio nadie va a "reclamar".
- **Cuándo romperla**: el ahogado está "cableado" porque se detecta con la misma consulta que el mate;
  separarlo en una regla obligaría a calcular dos veces las jugadas legales.

### 8. Los estados no saben describirse

- **Qué**: `EstadoPartida` expone turno, transiciones y si terminó; los textos ("¡JAQUE MATE! Ganaron las
  Blancas") están en `TextosPartida`, en la interfaz. Lo mismo con los símbolos Unicode de las piezas.
- **Por qué**: una regla del núcleo no debería cambiar porque la interfaz quiera otro texto u otro idioma.
  `EstadoTablas` lleva el `MotivoTablas` y cada adaptador lo traduce como quiere.
- **Cuándo romperla**: si hubiera un solo adaptador para siempre, el texto en el estado sería más corto.
  Ya hay dos (ventana y consola) y los tests, así que la separación paga.

### 9. Observer para avisar, valor de retorno para responder

- **Qué**: `intentarMover` devuelve el `EventoJugada` a quien movió, y además lo publica a los
  `ObservadorPartida` suscriptos (la lista de jugadas, el registro por consola).
- **Por qué**: quien mueve necesita la respuesta en el acto (¿fue legal?, ¿capturó?); los que solo quieren
  enterarse no deberían obligar a la partida a conocerlos. Agregar un observador nuevo (un PGN, un sonido)
  no toca el núcleo. La ventana se desuscribe al cerrarse para no dejar referencias colgadas.
- **Cuándo romperla**: `SesionDePartida` no se suscribe; usa el retorno porque necesita el resultado
  síncrono para la XP. Suscribirla sería forzar el patrón donde no hace falta.

### 10. La IA asesora, no arbitra

- **Qué**: `MotorAjedrez` es un puerto; `SesionDePartida.calcularJugadaIA` valida la sugerencia contra
  las jugadas legales del núcleo, y si el motor falla, no responde o propone algo ilegal juega una jugada
  legal de respaldo. Cada decisión se registra (`RegistroDecisionesIA`) y la ventana avisa cuando hubo respaldo.
- **Por qué**: la salida de un motor externo es una entrada no confiable, y el juego tiene que continuar
  aunque la IA se caiga. Que el motor sea Python detrás de un proceso es un detalle del adaptador: se
  cambia por otro sin tocar el núcleo.
- **Cuándo romperla**: para un motor embebido y determinista, el registro de decisiones sería ruido.
  Con un proceso externo que puede morir, vale.

### 11. Tests sin framework, en Arrange-Act-Assert, con dobles de prueba

- **Qué**: `ajedrez.tests` corre con `java` pelado. Usa un fake (repositorio en memoria), stubs (motores
  que devuelven una jugada fija, selector de promoción fijo) y mocks (observador y registro que cuentan llamadas).
- **Por qué**: el núcleo se prueba sin ninguna infraestructura; que no haya ni siquiera una biblioteca de
  tests que instalar hace que cualquiera los corra con `correr_tests.bat`.
- **Cuándo romperla**: en un equipo más grande, JUnit da mejores reportes y ejecución selectiva; el salto
  es mecánico porque los tests ya están escritos como funciones independientes.

### 12. Persistencia por serialización Java, contraseñas con PBKDF2

- **Qué**: `RepositorioPerfilesArchivo` serializa la lista de perfiles en `perfiles.dat`; las contraseñas
  se guardan como sal + hash PBKDF2 y se comparan en tiempo constante.
- **Por qué**: cero dependencias y suficiente para una aplicación local. Guardar contraseñas en claro no
  es aceptable ni en una aplicación chica.
- **Cuándo romperla**: con varios usuarios concurrentes o datos compartidos, el repositorio pasaría a una
  base de datos. Es un adaptador: cambia una clase y una línea de `Main`.
