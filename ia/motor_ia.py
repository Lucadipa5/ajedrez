"""
Motor de IA de ajedrez para el proyecto (adaptador en Python).

Usa la librería python-chess (pip install chess) para generar las jugadas legales
y una búsqueda alfa-beta con evaluación por material y posición para elegir la mejor.
Si hay un Stockfish instalado (variable de entorno STOCKFISH_PATH o "stockfish" en el PATH),
en dificultad Difícil se usa ese motor.

Protocolo (una línea por mensaje, por stdin/stdout):
  - Al iniciar imprime "LISTO", o "ERROR <motivo>" si falta la librería.
  - Recibe:   "<dificultad> <fen>"    (dificultad: FACIL | MEDIA | DIFICIL)
  - Responde: la jugada en notación UCI (ej. "e2e4", "e7e8q"), o "ERROR <motivo>".
"""

import os
import random
import shutil
import sys

try:
    import chess
    import chess.engine
except ImportError:
    print("ERROR Falta la librería python-chess. Instalala con: pip install chess", flush=True)
    sys.exit(1)


VALOR_PIEZA = {
    chess.PAWN: 100, chess.KNIGHT: 320, chess.BISHOP: 330,
    chess.ROOK: 500, chess.QUEEN: 900, chess.KING: 0,
}

# Tablas de posición (vista de las blancas, casilla a8 primero). Premian centralizar
# piezas, avanzar peones y mantener al rey protegido.
TABLAS = {
    chess.PAWN: [
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0],
    chess.KNIGHT: [
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50],
    chess.BISHOP: [
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20],
    chess.ROOK: [
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10, 10, 10, 10, 10,  5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
          0,  0,  0,  5,  5,  0,  0,  0],
    chess.QUEEN: [
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20],
    chess.KING: [
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20],
}

MATE = 100000

# Profundidad de búsqueda y "ruido" (en centipeones) por dificultad
CONFIG = {
    "FACIL":   {"profundidad": 1, "ruido": 120},
    "MEDIA":   {"profundidad": 2, "ruido": 15},
    "DIFICIL": {"profundidad": 4, "ruido": 0},
}


def evaluar(tablero):
    """Puntaje desde el punto de vista del bando que mueve."""
    puntaje = 0
    for casilla, pieza in tablero.piece_map().items():
        # Las tablas están escritas desde a8; para las blancas se espeja la fila
        indice = chess.square_mirror(casilla) if pieza.color == chess.WHITE else casilla
        valor = VALOR_PIEZA[pieza.piece_type] + TABLAS[pieza.piece_type][indice]
        puntaje += valor if pieza.color == chess.WHITE else -valor
    return puntaje if tablero.turn == chess.WHITE else -puntaje


def ordenar(tablero, jugadas):
    """Capturas primero (víctima valiosa con atacante barato), después coronaciones."""
    def clave(jugada):
        if tablero.is_capture(jugada):
            victima = tablero.piece_at(jugada.to_square)
            atacante = tablero.piece_at(jugada.from_square)
            v = VALOR_PIEZA[victima.piece_type] if victima else 100
            return -(10 * v - VALOR_PIEZA[atacante.piece_type])
        return -800 if jugada.promotion else 0
    return sorted(jugadas, key=clave)


def quiescencia(tablero, alfa, beta, prof=0):
    """Sigue analizando capturas para no cortar la búsqueda en medio de un intercambio."""
    base = evaluar(tablero)
    if base >= beta or prof >= 4:
        return base
    alfa = max(alfa, base)
    for jugada in ordenar(tablero, [j for j in tablero.legal_moves if tablero.is_capture(j)]):
        tablero.push(jugada)
        puntaje = -quiescencia(tablero, -beta, -alfa, prof + 1)
        tablero.pop()
        if puntaje >= beta:
            return puntaje
        alfa = max(alfa, puntaje)
    return alfa


def negamax(tablero, profundidad, alfa, beta):
    if tablero.is_checkmate():
        return -MATE - profundidad  # preferir el mate más rápido
    if tablero.is_stalemate() or tablero.is_insufficient_material():
        return 0
    if profundidad == 0:
        return quiescencia(tablero, alfa, beta)
    mejor = -MATE * 2
    for jugada in ordenar(tablero, list(tablero.legal_moves)):
        tablero.push(jugada)
        puntaje = -negamax(tablero, profundidad - 1, -beta, -alfa)
        tablero.pop()
        mejor = max(mejor, puntaje)
        alfa = max(alfa, puntaje)
        if alfa >= beta:
            break
    return mejor


def elegir_jugada(tablero, dificultad):
    config = CONFIG.get(dificultad, CONFIG["MEDIA"])
    mejor_jugada, mejor_puntaje = None, -MATE * 3
    alfa, beta = -MATE * 3, MATE * 3
    for jugada in ordenar(tablero, list(tablero.legal_moves)):
        tablero.push(jugada)
        puntaje = -negamax(tablero, config["profundidad"] - 1, -beta, -alfa)
        tablero.pop()
        puntaje += random.randint(-config["ruido"], config["ruido"])
        if puntaje > mejor_puntaje:
            mejor_jugada, mejor_puntaje = jugada, puntaje
        alfa = max(alfa, puntaje - config["ruido"])
    return mejor_jugada


def abrir_stockfish():
    ruta = os.environ.get("STOCKFISH_PATH") or shutil.which("stockfish")
    if not ruta:
        return None
    try:
        return chess.engine.SimpleEngine.popen_uci(ruta)
    except Exception:
        return None


def main():
    stockfish = abrir_stockfish()
    print("LISTO", flush=True)
    for linea in sys.stdin:
        linea = linea.strip()
        if not linea:
            continue
        if linea == "SALIR":
            break
        try:
            dificultad, fen = linea.split(" ", 1)
            tablero = chess.Board(fen)
            if not any(tablero.legal_moves):
                print("ERROR sin jugadas legales", flush=True)
                continue
            if stockfish and dificultad == "DIFICIL":
                jugada = stockfish.play(tablero, chess.engine.Limit(time=0.5)).move
            else:
                jugada = elegir_jugada(tablero, dificultad)
            print(jugada.uci(), flush=True)
        except Exception as e:  # nunca cortar el proceso por una posición rara
            print("ERROR " + str(e).replace("\n", " "), flush=True)
    if stockfish:
        stockfish.quit()


if __name__ == "__main__":
    main()
