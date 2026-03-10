package com.codexaga;

public final class GameConfig
{
    public static final int WIDTH = 800;
    public static final int HEIGHT = 900;

    public static final int HUD_HEIGHT = 74;
    public static final int PLAYFIELD_MARGIN = 52;
    public static final double PLAYFIELD_TOP = HUD_HEIGHT + 12;
    public static final double PLAYFIELD_BOTTOM = HEIGHT - 36;
    public static final double PLAYFIELD_LEFT = PLAYFIELD_MARGIN;
    public static final double PLAYFIELD_RIGHT = WIDTH - PLAYFIELD_MARGIN;

    public static final int STAR_COUNT = 160;

    public static final double PLAYER_WIDTH = 46;
    public static final double PLAYER_HEIGHT = 28;
    public static final double DUAL_PLAYER_WIDTH = 82;
    public static final double PLAYER_SPEED = 340;
    public static final double PLAYER_FIRE_COOLDOWN = 0.2;
    public static final double PLAYER_INVULNERABLE_TIME = 2.0;
    public static final double RESCUE_MAGNET_RADIUS = 46;

    public static final int ENEMY_ROWS = 5;
    public static final int ENEMY_COLS = 10;
    public static final double ENEMY_WIDTH = 36;
    public static final double ENEMY_HEIGHT = 24;
    public static final double ENEMY_HORIZONTAL_GAP = 16;
    public static final double ENEMY_VERTICAL_GAP = 14;
    public static final double ENEMY_FORMATION_SPEED = 54;
    public static final double ENEMY_DROP_DISTANCE = 18;
    public static final double ENEMY_FIRE_COOLDOWN = 1.05;
    public static final double ENEMY_DIVE_SPEED = 168;
    public static final double ENEMY_RETURN_SPEED = 226;
    public static final double CAPTURE_BEAM_Y = PLAYFIELD_BOTTOM - 190;
    public static final double CAPTURE_BEAM_WIDTH = 92;
    public static final double CAPTURE_BEAM_DURATION = 2.6;
    public static final double CAPTURE_BEAM_SPEED = 188;

    public static final double BULLET_RADIUS = 4;
    public static final double PLAYER_BULLET_SPEED = -540;
    public static final double ENEMY_BULLET_SPEED = 290;

    public static final int STARTING_LIVES = 3;

    private GameConfig()
    {
    }
}
