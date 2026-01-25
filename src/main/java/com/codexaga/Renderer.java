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
                new Stop(1, Color.rgb(4, 6, 18)));
        gc.setFill(gradient);
        gc.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

        for (Star star : state.getStars())
        {
            double alpha = 0.35 + Math.min(0.6, star.getRadius() / 2.6);
            gc.setFill(Color.color(0.9, 0.95, 1.0, alpha));
            gc.fillOval(star.getX(), star.getY(), star.getRadius(), star.getRadius());
        }
    }

    private void drawHud(GraphicsContext gc, GameState state)
    {
        gc.setFill(Color.rgb(12, 20, 44, 0.85));
        gc.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HUD_HEIGHT);

        gc.setFill(Color.rgb(120, 210, 255));
        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("Score " + state.getScore(), 24, GameConfig.HUD_HEIGHT / 2.0);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Level " + state.getLevel(), GameConfig.WIDTH / 2.0, GameConfig.HUD_HEIGHT / 2.0);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("Lives " + state.getLives(), GameConfig.WIDTH - 24, GameConfig.HUD_HEIGHT / 2.0);
    }

    private void drawPlayfield(GraphicsContext gc, GameState state)
    {
        gc.setStroke(Color.rgb(60, 80, 120, 0.8));
        gc.setLineWidth(2);
        gc.strokeRoundRect(GameConfig.PLAYFIELD_LEFT, GameConfig.PLAYFIELD_TOP,
                GameConfig.PLAYFIELD_RIGHT - GameConfig.PLAYFIELD_LEFT,
                GameConfig.PLAYFIELD_BOTTOM - GameConfig.PLAYFIELD_TOP, 18, 18);

        for (Enemy enemy : state.getEnemies())
        {
            if (!enemy.isAlive())
            {
                continue;
            }
            double x = state.getEnemyX(enemy);
            double y = state.getEnemyY(enemy);
            drawEnemy(gc, enemy.getRow(), x, y);
        }

        for (Bullet bullet : state.getBullets())
        {
            if (bullet.isFromPlayer())
            {
                gc.setFill(Color.rgb(120, 255, 240));
            }
            else
            {
                gc.setFill(Color.rgb(255, 150, 90));
            }
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
            double radius = 14 + (0.6 - t) * 26;
            gc.setStroke(Color.color(1.0, 0.7, 0.3, 0.7));
            gc.setLineWidth(2.0);
            gc.strokeOval(explosion.getX() - radius * 0.5, explosion.getY() - radius * 0.5, radius, radius);
        }
    }

    private void drawEnemy(GraphicsContext gc, int row, double x, double y)
    {
        Color body;
        if (row <= 1)
        {
            body = Color.rgb(120, 255, 220);
        }
        else if (row == 2)
        {
            body = Color.rgb(130, 190, 255);
        }
        else
        {
            body = Color.rgb(255, 170, 120);
        }

        double halfW = GameConfig.ENEMY_WIDTH * 0.5;
        double halfH = GameConfig.ENEMY_HEIGHT * 0.5;

        gc.setFill(body);
        gc.fillRoundRect(x - halfW, y - halfH, GameConfig.ENEMY_WIDTH, GameConfig.ENEMY_HEIGHT, 8, 8);
        gc.setFill(body.brighter());
        gc.fillOval(x - 10, y - 6, 6, 6);
        gc.fillOval(x + 4, y - 6, 6, 6);
        gc.setStroke(body.darker());
        gc.setLineWidth(2);
        gc.strokeLine(x - halfW, y, x - halfW - 8, y + 6);
        gc.strokeLine(x + halfW, y, x + halfW + 8, y + 6);
    }

    private void drawPlayer(GraphicsContext gc, PlayerShip player)
    {
        double x = player.getX();
        double y = player.getY();
        double halfW = GameConfig.PLAYER_WIDTH * 0.5;
        double halfH = GameConfig.PLAYER_HEIGHT * 0.5;

        Color hull = Color.rgb(120, 210, 255);
        if (player.isInvulnerable())
        {
            hull = Color.rgb(200, 230, 255, 0.7);
        }

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

        gc.setFill(Color.rgb(8, 12, 24, 0.7));
        gc.fillRect(0, GameConfig.HUD_HEIGHT, GameConfig.WIDTH, GameConfig.HEIGHT - GameConfig.HUD_HEIGHT);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        if (state.getStatus() == GameState.Status.READY)
        {
            gc.setFill(Color.rgb(120, 210, 255));
            gc.setFont(titleFont);
            gc.fillText("CODEXAGA", GameConfig.WIDTH / 2.0, GameConfig.HEIGHT / 2.0 - 20);

            gc.setFont(infoFont);
            gc.fillText("Press Enter or Fire to launch", GameConfig.WIDTH / 2.0, GameConfig.HEIGHT / 2.0 + 30);
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
