package com.codexaga;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public final class Renderer
{
    private final Font hudFont = Font.font("Consolas", FontWeight.BOLD, 20);
    private final Font statusFont = Font.font("Consolas", FontWeight.BOLD, 15);
    private final Font titleFont = Font.font("Consolas", FontWeight.EXTRA_BOLD, 40);
    private final Font infoFont = Font.font("Consolas", FontWeight.SEMI_BOLD, 18);

    public void render(GraphicsContext gc, GameState state)
    {
        drawBackground(gc, state);
        drawHud(gc, state);
        drawPlayfield(gc, state);
        drawOverlay(gc, state);
    }

    private void drawBackground(GraphicsContext gc, GameState state)
    {
        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(10, 16, 38)),
                new Stop(0.55, Color.rgb(6, 10, 24)),
                new Stop(1, Color.rgb(4, 6, 18)));
        gc.setFill(gradient);
        gc.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

        for (Star star : state.getStars())
        {
            double alpha = 0.3 + Math.min(0.65, star.getRadius() / 2.4);
            gc.setFill(Color.color(0.92, 0.96, 1.0, alpha));
            gc.fillOval(star.getX(), star.getY(), star.getRadius(), star.getRadius());
        }
    }

    private void drawHud(GraphicsContext gc, GameState state)
    {
        gc.setFill(Color.rgb(12, 20, 44, 0.88));
        gc.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HUD_HEIGHT);

        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(hudFont);
        gc.setFill(Color.rgb(120, 210, 255));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Score " + state.getScore(), 22, 25);
        gc.fillText("Ships " + state.getLives(), 22, 52);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Level " + state.getLevel(), GameConfig.WIDTH / 2.0, 25);

        gc.setFont(statusFont);
        String playerStatus = "Single";
        PlayerShip player = state.getPlayer();
        if (player != null && player.isDualFighter())
        {
            playerStatus = "Dual Fighter";
        }
        else if (state.hasCapturedShipInBoss())
        {
            playerStatus = "Captured - shoot the boss";
        }
        else if (state.getRescueShip() != null)
        {
            playerStatus = "Rescue incoming";
        }
        gc.fillText(playerStatus, GameConfig.WIDTH / 2.0, 52);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFont(hudFont);
        gc.fillText("P pause", GameConfig.WIDTH - 22, 25);
        gc.setFont(statusFont);
        gc.fillText("R restart", GameConfig.WIDTH - 22, 52);
    }

    private void drawPlayfield(GraphicsContext gc, GameState state)
    {
        gc.setStroke(Color.rgb(60, 80, 120, 0.85));
        gc.setLineWidth(2);
        gc.strokeRoundRect(GameConfig.PLAYFIELD_LEFT, GameConfig.PLAYFIELD_TOP,
                GameConfig.PLAYFIELD_RIGHT - GameConfig.PLAYFIELD_LEFT,
                GameConfig.PLAYFIELD_BOTTOM - GameConfig.PLAYFIELD_TOP, 18, 18);

        drawCaptureBeams(gc, state);

        for (Enemy enemy : state.getEnemies())
        {
            if (!enemy.isAlive())
            {
                continue;
            }
            double x = state.getEnemyX(enemy);
            double y = state.getEnemyY(enemy);
            drawEnemy(gc, enemy, x, y);
        }

        RescueShip rescueShip = state.getRescueShip();
        if (rescueShip != null)
        {
            drawRescueShip(gc, rescueShip);
        }

        for (Bullet bullet : state.getBullets())
        {
            gc.setFill(bullet.isFromPlayer() ? Color.rgb(120, 255, 240) : Color.rgb(255, 150, 90));
            gc.fillOval(bullet.getX() - GameConfig.BULLET_RADIUS, bullet.getY() - GameConfig.BULLET_RADIUS,
                    GameConfig.BULLET_RADIUS * 2, GameConfig.BULLET_RADIUS * 2);
        }

        PlayerShip player = state.getPlayer();
        if (player != null)
        {
            drawPlayer(gc, player);
        }

        for (Explosion explosion : state.getExplosions())
        {
            double t = explosion.getTimer();
            double radius = 18 + (0.62 - Math.min(0.62, t)) * 34;
            gc.setStroke(Color.color(1.0, 0.75, 0.32, 0.72));
            gc.setLineWidth(2.0);
            gc.strokeOval(explosion.getX() - radius * 0.5, explosion.getY() - radius * 0.5, radius, radius);
        }
    }

    private void drawCaptureBeams(GraphicsContext gc, GameState state)
    {
        for (Enemy enemy : state.getEnemies())
        {
            if (!enemy.isAlive() || enemy.getMode() != EnemyMode.CAPTURE_BEAM)
            {
                continue;
            }

            double x = state.getEnemyX(enemy);
            double y = state.getEnemyY(enemy) + GameConfig.ENEMY_HEIGHT * 0.5;
            double beamHeight = GameConfig.PLAYFIELD_BOTTOM - y;
            LinearGradient beam = new LinearGradient(0, y, 0, y + beamHeight, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(0.95, 0.95, 1.0, 0.68)),
                    new Stop(0.35, Color.color(0.4, 0.95, 1.0, 0.22)),
                    new Stop(1, Color.color(0.2, 0.75, 1.0, 0.03)));
            gc.setFill(beam);
            gc.fillPolygon(
                    new double[] { x - 14, x + 14, x + GameConfig.CAPTURE_BEAM_WIDTH * 0.5, x - GameConfig.CAPTURE_BEAM_WIDTH * 0.5 },
                    new double[] { y, y, y + beamHeight, y + beamHeight },
                    4);
        }
    }

    private void drawEnemy(GraphicsContext gc, Enemy enemy, double x, double y)
    {
        Color body;
        Color accent;
        switch (enemy.getKind())
        {
        case BOSS -> {
            body = Color.rgb(255, 170, 96);
            accent = Color.rgb(255, 232, 120);
        }
        case ESCORT -> {
            body = Color.rgb(126, 190, 255);
            accent = Color.rgb(180, 232, 255);
        }
        case GRUNT -> {
            body = Color.rgb(120, 255, 220);
            accent = Color.rgb(220, 255, 250);
        }
        default -> throw new IllegalStateException("Unexpected enemy kind");
        }

        double halfW = GameConfig.ENEMY_WIDTH * 0.5;
        double halfH = GameConfig.ENEMY_HEIGHT * 0.5;

        gc.setFill(body);
        gc.fillRoundRect(x - halfW, y - halfH, GameConfig.ENEMY_WIDTH, GameConfig.ENEMY_HEIGHT, 8, 8);
        gc.setFill(accent);
        gc.fillOval(x - 10, y - 6, 6, 6);
        gc.fillOval(x + 4, y - 6, 6, 6);
        gc.setStroke(body.darker());
        gc.setLineWidth(2);
        gc.strokeLine(x - halfW + 2, y + 1, x - halfW - 8, y + 7);
        gc.strokeLine(x + halfW - 2, y + 1, x + halfW + 8, y + 7);

        if (enemy.getKind() == EnemyKind.BOSS)
        {
            gc.setFill(Color.rgb(255, 220, 90));
            gc.fillRect(x - 8, y - halfH - 5, 16, 4);
        }

        if (enemy.isCarryingCapturedShip())
        {
            gc.setStroke(Color.rgb(150, 220, 255));
            gc.setLineWidth(2);
            gc.strokeLine(x, y + halfH, x, y + 28);
            drawMiniFighter(gc, x, y + 36, Color.rgb(140, 220, 255));
        }
    }

    private void drawRescueShip(GraphicsContext gc, RescueShip rescueShip)
    {
        gc.setStroke(Color.rgb(150, 235, 255, 0.75));
        gc.setLineWidth(1.5);
        gc.strokeOval(rescueShip.getX() - 16, rescueShip.getY() - 16, 32, 32);
        drawMiniFighter(gc, rescueShip.getX(), rescueShip.getY(), Color.rgb(150, 225, 255));
    }

    private void drawMiniFighter(GraphicsContext gc, double x, double y, Color hull)
    {
        gc.setFill(hull);
        gc.fillPolygon(
                new double[] { x - 10, x, x + 10 },
                new double[] { y + 8, y - 8, y + 8 },
                3);
        gc.setFill(Color.rgb(255, 225, 130));
        gc.fillOval(x - 3, y - 2, 6, 4);
    }

    private void drawPlayer(GraphicsContext gc, PlayerShip player)
    {
        if (player.isDualFighter())
        {
            drawDualPlayer(gc, player);
            return;
        }

        drawSinglePlayer(gc, player, player.getX(), Color.rgb(120, 210, 255));
    }

    private void drawDualPlayer(GraphicsContext gc, PlayerShip player)
    {
        Color left = Color.rgb(120, 210, 255, player.isInvulnerable() ? 0.72 : 1.0);
        Color right = Color.rgb(120, 255, 220, player.isInvulnerable() ? 0.72 : 1.0);
        drawSinglePlayer(gc, player, player.getX() - 18, left);
        drawSinglePlayer(gc, player, player.getX() + 18, right);
        gc.setStroke(Color.rgb(150, 235, 255, 0.65));
        gc.setLineWidth(2);
        gc.strokeLine(player.getX() - 8, player.getY() + 8, player.getX() + 8, player.getY() + 8);
    }

    private void drawSinglePlayer(GraphicsContext gc, PlayerShip player, double x, Color baseHull)
    {
        double y = player.getY();
        double halfW = GameConfig.PLAYER_WIDTH * 0.5;
        double halfH = GameConfig.PLAYER_HEIGHT * 0.5;

        Color hull = player.isInvulnerable()
                ? Color.color(baseHull.getRed(), baseHull.getGreen(), baseHull.getBlue(), 0.72)
                : baseHull;

        gc.setFill(hull);
        gc.fillPolygon(
                new double[] { x - halfW, x, x + halfW },
                new double[] { y + halfH, y - halfH, y + halfH },
                3);
        gc.setFill(Color.rgb(255, 220, 120));
        gc.fillOval(x - 6, y - 4, 12, 8);
        gc.setStroke(Color.rgb(60, 120, 160));
        gc.setLineWidth(2);
        gc.strokeLine(x - halfW + 6, y + halfH - 2, x - halfW - 10, y + halfH + 8);
        gc.strokeLine(x + halfW - 6, y + halfH - 2, x + halfW + 10, y + halfH + 8);
    }

    private void drawOverlay(GraphicsContext gc, GameState state)
    {
        if (state.getStatus() == GameState.Status.PLAYING)
        {
            return;
        }

        gc.setFill(Color.rgb(8, 12, 24, 0.72));
        gc.fillRect(0, GameConfig.HUD_HEIGHT, GameConfig.WIDTH, GameConfig.HEIGHT - GameConfig.HUD_HEIGHT);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        if (state.getStatus() == GameState.Status.READY)
        {
            gc.setFill(Color.rgb(120, 210, 255));
            gc.setFont(titleFont);
            gc.fillText("CODEXAGA", GameConfig.WIDTH / 2.0, GameConfig.HEIGHT / 2.0 - 34);

            gc.setFont(infoFont);
            gc.fillText("Enter or Fire to launch", GameConfig.WIDTH / 2.0, GameConfig.HEIGHT / 2.0 + 12);
            gc.fillText("Bosses can capture your fighter - rescue it for dual guns", GameConfig.WIDTH / 2.0,
                    GameConfig.HEIGHT / 2.0 + 44);
        }
        else if (state.getStatus() == GameState.Status.PAUSED)
        {
            gc.setFill(Color.rgb(255, 220, 160));
            gc.setFont(titleFont);
            gc.fillText("PAUSED", GameConfig.WIDTH / 2.0, GameConfig.HEIGHT / 2.0 - 10);
            gc.setFont(infoFont);
            gc.fillText("Press P to resume", GameConfig.WIDTH / 2.0, GameConfig.HEIGHT / 2.0 + 26);
        }
        else if (state.getStatus() == GameState.Status.GAME_OVER)
        {
            gc.setFill(Color.rgb(255, 150, 120));
            gc.setFont(titleFont);
            gc.fillText("GAME OVER", GameConfig.WIDTH / 2.0, GameConfig.HEIGHT / 2.0 - 10);
            gc.setFont(infoFont);
            gc.fillText("Press R to restart", GameConfig.WIDTH / 2.0, GameConfig.HEIGHT / 2.0 + 26);
        }
    }
}
