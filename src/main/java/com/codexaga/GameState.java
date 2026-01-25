package com.codexaga;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class GameState
{
    public enum Status
    {
        READY,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private final Random random;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Explosion> explosions = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();

    private PlayerShip player;
    private int score;
    private int lives;
    private int level;
    private Status status = Status.READY;

    private double fireCooldown;
    private double enemyFireCooldown;
    private double formationOffsetX;
    private double formationOffsetY;
    private double formationDirection = 1;
    private double formationSpeed;

    public GameState(Random random)
    {
        this.random = random;
        initStars();
        startNewGame();
    }

    public void startNewGame()
    {
        score = 0;
        level = 1;
        lives = GameConfig.STARTING_LIVES;
        status = Status.READY;
        bullets.clear();
        explosions.clear();
        spawnPlayer();
        spawnWave();
    }

    public void togglePause()
    {
        if (status == Status.PLAYING)
        {
            status = Status.PAUSED;
        }
        else if (status == Status.PAUSED)
        {
            status = Status.PLAYING;
        }
    }

    public void update(double delta, InputState input)
    {
        updateStars(delta);

        if (input.consumeRestart())
        {
            startNewGame();
        }

        if (input.consumePause())
        {
            togglePause();
        }

        if (status == Status.READY)
        {
            if (input.consumeStart() || input.isFire() || input.isLeft() || input.isRight())
            {
                status = Status.PLAYING;
            }
        }

        if (status != Status.PLAYING)
        {
            return;
        }

        updatePlayer(delta, input);
        updateFormation(delta);
        updateEnemyFire(delta);
        updateBullets(delta);
        resolveCollisions();
        updateExplosions(delta);

        if (enemies.stream().noneMatch(Enemy::isAlive))
        {
            level += 1;
            spawnWave();
        }
    }

    private void updatePlayer(double delta, InputState input)
    {
        player.update(delta);

        double direction = 0;
        if (input.isLeft())
        {
            direction -= 1;
        }
        if (input.isRight())
        {
            direction += 1;
        }

        if (direction != 0)
        {
            double nextX = player.getX() + direction * GameConfig.PLAYER_SPEED * delta;
            double minX = GameConfig.PLAYFIELD_LEFT + GameConfig.PLAYER_WIDTH * 0.5;
            double maxX = GameConfig.PLAYFIELD_RIGHT - GameConfig.PLAYER_WIDTH * 0.5;
            player.setX(Math.max(minX, Math.min(maxX, nextX)));
        }

        if (fireCooldown > 0)
        {
            fireCooldown = Math.max(0, fireCooldown - delta);
        }

        if (input.isFire() && fireCooldown <= 0)
        {
            fireCooldown = GameConfig.PLAYER_FIRE_COOLDOWN;
            bullets.add(new Bullet(player.getX(), player.getY() - GameConfig.PLAYER_HEIGHT * 0.6,
                    GameConfig.PLAYER_BULLET_SPEED, true));
        }
    }

    private void updateFormation(double delta)
    {
        formationSpeed = GameConfig.ENEMY_FORMATION_SPEED + (level - 1) * 10;
        double move = formationSpeed * formationDirection * delta;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (Enemy enemy : enemies)
        {
            if (!enemy.isAlive())
            {
                continue;
            }
            double x = getEnemyX(enemy);
            minX = Math.min(minX, x - GameConfig.ENEMY_WIDTH * 0.5);
            maxX = Math.max(maxX, x + GameConfig.ENEMY_WIDTH * 0.5);
        }

        double leftBound = GameConfig.PLAYFIELD_LEFT + 10;
        double rightBound = GameConfig.PLAYFIELD_RIGHT - 10;

        if (maxX + move > rightBound || minX + move < leftBound)
        {
            formationDirection *= -1;
            formationOffsetY += GameConfig.ENEMY_DROP_DISTANCE;
        }
        else
        {
            formationOffsetX += move;
        }

        double dangerLine = player.getY() - GameConfig.PLAYER_HEIGHT * 0.5;
        for (Enemy enemy : enemies)
        {
            if (!enemy.isAlive())
            {
                continue;
            }
            if (getEnemyY(enemy) + GameConfig.ENEMY_HEIGHT * 0.5 >= dangerLine)
            {
                status = Status.GAME_OVER;
                break;
            }
        }
    }

    private void updateEnemyFire(double delta)
    {
        enemyFireCooldown -= delta;
        if (enemyFireCooldown > 0)
        {
            return;
        }

        List<Enemy> alive = enemies.stream().filter(Enemy::isAlive).toList();
        if (alive.isEmpty())
        {
            return;
        }

        Enemy shooter = alive.get(random.nextInt(alive.size()));
        bullets.add(new Bullet(getEnemyX(shooter), getEnemyY(shooter) + GameConfig.ENEMY_HEIGHT * 0.6,
                GameConfig.ENEMY_BULLET_SPEED + level * 12, false));

        enemyFireCooldown = Math.max(0.35, GameConfig.ENEMY_FIRE_COOLDOWN - level * 0.05);
    }

    private void updateBullets(double delta)
    {
        bullets.removeIf(bullet ->
        {
            bullet.update(delta);
            return bullet.getY() < GameConfig.PLAYFIELD_TOP - 20
                    || bullet.getY() > GameConfig.PLAYFIELD_BOTTOM + 20;
        });
    }

    private void resolveCollisions()
    {
        double playerHalfW = GameConfig.PLAYER_WIDTH * 0.5;
        double playerHalfH = GameConfig.PLAYER_HEIGHT * 0.5;

        bullets.removeIf(bullet ->
        {
            if (bullet.isFromPlayer())
            {
                for (Enemy enemy : enemies)
                {
                    if (!enemy.isAlive())
                    {
                        continue;
                    }
                    double ex = getEnemyX(enemy);
                    double ey = getEnemyY(enemy);
                    if (Math.abs(bullet.getX() - ex) <= GameConfig.ENEMY_WIDTH * 0.5
                            && Math.abs(bullet.getY() - ey) <= GameConfig.ENEMY_HEIGHT * 0.5)
                    {
                        enemy.kill();
                        explosions.add(new Explosion(ex, ey, 0.35));
                        score += 120 + (GameConfig.ENEMY_ROWS - enemy.getRow()) * 15;
                        return true;
                    }
                }
            }
            else
            {
                if (!player.isInvulnerable())
                {
                    if (Math.abs(bullet.getX() - player.getX()) <= playerHalfW
                            && Math.abs(bullet.getY() - player.getY()) <= playerHalfH)
                    {
                        loseLife();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void updateExplosions(double delta)
    {
        explosions.removeIf(explosion ->
        {
            explosion.update(delta);
            return explosion.isDone();
        });
    }

    private void loseLife()
    {
        explosions.add(new Explosion(player.getX(), player.getY(), 0.6));
        lives -= 1;
        if (lives <= 0)
        {
            status = Status.GAME_OVER;
            return;
        }
        spawnPlayer();
    }

    private void spawnPlayer()
    {
        player = new PlayerShip((GameConfig.PLAYFIELD_LEFT + GameConfig.PLAYFIELD_RIGHT) * 0.5,
                GameConfig.PLAYFIELD_BOTTOM - 10);
        player.setInvulnerable(GameConfig.PLAYER_INVULNERABLE_TIME);
        fireCooldown = 0;
    }

    private void spawnWave()
    {
        enemies.clear();
        formationOffsetX = 0;
        formationOffsetY = 0;
        formationDirection = random.nextBoolean() ? 1 : -1;
        enemyFireCooldown = 1.0;

        for (int row = 0; row < GameConfig.ENEMY_ROWS; row++)
        {
            for (int col = 0; col < GameConfig.ENEMY_COLS; col++)
            {
                enemies.add(new Enemy(row, col));
            }
        }
    }

    private void initStars()
    {
        stars.clear();
        for (int i = 0; i < GameConfig.STAR_COUNT; i++)
        {
            double x = random.nextDouble() * GameConfig.WIDTH;
            double y = random.nextDouble() * GameConfig.HEIGHT;
            double speed = 20 + random.nextDouble() * 80;
            double radius = 0.8 + random.nextDouble() * 1.6;
            stars.add(new Star(x, y, speed, radius));
        }
        stars.sort(Comparator.comparingDouble(Star::getRadius));
    }

    private void updateStars(double delta)
    {
        for (Star star : stars)
        {
            star.update(delta);
        }
    }

    public double getEnemyX(Enemy enemy)
    {
        double formationWidth = GameConfig.ENEMY_COLS * GameConfig.ENEMY_WIDTH
                + (GameConfig.ENEMY_COLS - 1) * GameConfig.ENEMY_HORIZONTAL_GAP;
        double startX = (GameConfig.PLAYFIELD_LEFT + GameConfig.PLAYFIELD_RIGHT - formationWidth) * 0.5
                + GameConfig.ENEMY_WIDTH * 0.5;
        return startX + enemy.getCol() * (GameConfig.ENEMY_WIDTH + GameConfig.ENEMY_HORIZONTAL_GAP) + formationOffsetX;
    }

    public double getEnemyY(Enemy enemy)
    {
        double startY = GameConfig.PLAYFIELD_TOP + 30;
        return startY + enemy.getRow() * (GameConfig.ENEMY_HEIGHT + GameConfig.ENEMY_VERTICAL_GAP) + formationOffsetY;
    }

    public List<Enemy> getEnemies()
    {
        return enemies;
    }

    public List<Bullet> getBullets()
    {
        return bullets;
    }

    public List<Star> getStars()
    {
        return stars;
    }

    public List<Explosion> getExplosions()
    {
        return explosions;
    }

    public PlayerShip getPlayer()
    {
        return player;
    }

    public int getScore()
    {
        return score;
    }

    public int getLives()
    {
        return lives;
    }

    public int getLevel()
    {
        return level;
    }

    public Status getStatus()
    {
        return status;
    }
}
