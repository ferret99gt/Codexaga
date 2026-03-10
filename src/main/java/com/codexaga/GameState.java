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
    private RescueShip rescueShip;
    private int score;
    private int lives;
    private int level;
    private Status status = Status.READY;

    private double fireCooldown;
    private double enemyFireCooldown;
    private double diveCooldown;
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
        rescueShip = null;
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
            updateExplosions(delta);
            return;
        }

        updatePlayer(delta, input);
        updateFormation(delta);
        updateEnemyAttacks(delta);
        updateEnemies(delta);
        updateBullets(delta);
        updateRescueShip(delta);
        resolveCollisions();
        updateExplosions(delta);

        if (status == Status.PLAYING && enemies.stream().noneMatch(Enemy::isAlive))
        {
            score += 300 * level;
            level += 1;
            spawnWave();
        }
    }

    private void updatePlayer(double delta, InputState input)
    {
        if (player == null)
        {
            return;
        }

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
            double halfWidth = player.getCollisionHalfWidth();
            double minX = GameConfig.PLAYFIELD_LEFT + halfWidth;
            double maxX = GameConfig.PLAYFIELD_RIGHT - halfWidth;
            player.setX(Math.max(minX, Math.min(maxX, nextX)));
        }

        if (fireCooldown > 0)
        {
            fireCooldown = Math.max(0, fireCooldown - delta);
        }

        int maxShots = player.isDualFighter() ? 4 : 2;
        if (input.isFire() && fireCooldown <= 0 && countPlayerBullets() < maxShots)
        {
            fireCooldown = GameConfig.PLAYER_FIRE_COOLDOWN;
            if (player.isDualFighter())
            {
                bullets.add(new Bullet(player.getX() - 16, player.getY() - GameConfig.PLAYER_HEIGHT * 0.55,
                        GameConfig.PLAYER_BULLET_SPEED, true));
                bullets.add(new Bullet(player.getX() + 16, player.getY() - GameConfig.PLAYER_HEIGHT * 0.55,
                        GameConfig.PLAYER_BULLET_SPEED, true));
            }
            else
            {
                bullets.add(new Bullet(player.getX(), player.getY() - GameConfig.PLAYER_HEIGHT * 0.6,
                        GameConfig.PLAYER_BULLET_SPEED, true));
            }
        }
    }

    private void updateFormation(double delta)
    {
        formationSpeed = GameConfig.ENEMY_FORMATION_SPEED + (level - 1) * 7;
        double move = formationSpeed * formationDirection * delta;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (Enemy enemy : enemies)
        {
            if (!enemy.isAlive() || enemy.getMode() != EnemyMode.FORMATION)
            {
                continue;
            }
            double x = getFormationX(enemy);
            minX = Math.min(minX, x - GameConfig.ENEMY_WIDTH * 0.5);
            maxX = Math.max(maxX, x + GameConfig.ENEMY_WIDTH * 0.5);
        }

        if (!Double.isFinite(minX) || !Double.isFinite(maxX))
        {
            return;
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
    }

    private void updateEnemyAttacks(double delta)
    {
        enemyFireCooldown -= delta;
        if (enemyFireCooldown <= 0)
        {
            Enemy shooter = chooseShooter();
            if (shooter != null)
            {
                bullets.add(new Bullet(getEnemyX(shooter), getEnemyY(shooter) + GameConfig.ENEMY_HEIGHT * 0.6,
                        GameConfig.ENEMY_BULLET_SPEED + level * 14, false));
            }
            enemyFireCooldown = Math.max(0.32, GameConfig.ENEMY_FIRE_COOLDOWN - level * 0.04);
        }

        diveCooldown -= delta;
        if (diveCooldown > 0)
        {
            return;
        }

        if (!tryStartCaptureDive())
        {
            tryStartDive();
        }

        diveCooldown = Math.max(0.8, 1.9 - level * 0.06);
    }

    private void updateEnemies(double delta)
    {
        for (Enemy enemy : enemies)
        {
            if (!enemy.isAlive())
            {
                continue;
            }

            switch (enemy.getMode())
            {
            case FORMATION -> {
            }
            case DIVE -> updateDive(enemy, delta);
            case CAPTURE_APPROACH -> updateCaptureApproach(enemy, delta);
            case CAPTURE_BEAM -> updateCaptureBeam(enemy, delta);
            case RETURNING -> updateReturn(enemy, delta);
            }
        }

        checkEnemyPressure();
    }

    private void updateDive(Enemy enemy, double delta)
    {
        enemy.advanceTimer(delta);
        double horizontal = enemy.getDiveDirection() * (88 + level * 5) * delta;
        horizontal += Math.cos(enemy.getTimer() * 4.8 + enemy.getCol() * 0.3) * 130 * delta;
        enemy.setX(clamp(enemy.getX() + horizontal,
                GameConfig.PLAYFIELD_LEFT + GameConfig.ENEMY_WIDTH * 0.5,
                GameConfig.PLAYFIELD_RIGHT - GameConfig.ENEMY_WIDTH * 0.5));
        enemy.setY(enemy.getY() + (GameConfig.ENEMY_DIVE_SPEED + level * 16) * delta);

        if (enemy.getY() < player.getY() - 80 && random.nextDouble() < delta * 0.55)
        {
            bullets.add(new Bullet(enemy.getX(), enemy.getY() + GameConfig.ENEMY_HEIGHT * 0.5,
                    GameConfig.ENEMY_BULLET_SPEED + level * 14, false));
        }

        if (enemy.getTimer() > 3.0 || enemy.getY() > GameConfig.PLAYFIELD_BOTTOM + 40)
        {
            beginReturn(enemy);
        }
    }

    private void updateCaptureApproach(Enemy enemy, double delta)
    {
        enemy.advanceTimer(delta);
        double correction = (enemy.getTargetX() - enemy.getX()) * Math.min(1, delta * 2.7);
        double curve = enemy.getDiveDirection() * Math.sin(enemy.getTimer() * 3.8) * 46 * delta;
        enemy.setX(clamp(enemy.getX() + correction + curve,
                GameConfig.PLAYFIELD_LEFT + GameConfig.ENEMY_WIDTH * 0.5,
                GameConfig.PLAYFIELD_RIGHT - GameConfig.ENEMY_WIDTH * 0.5));
        enemy.setY(enemy.getY() + (GameConfig.CAPTURE_BEAM_SPEED + level * 10) * delta);

        if (enemy.getY() >= GameConfig.CAPTURE_BEAM_Y)
        {
            enemy.setY(GameConfig.CAPTURE_BEAM_Y);
            enemy.setMode(EnemyMode.CAPTURE_BEAM);
            enemy.setTimer(GameConfig.CAPTURE_BEAM_DURATION);
        }
    }

    private void updateCaptureBeam(Enemy enemy, double delta)
    {
        enemy.setTimer(enemy.getTimer() - delta);
        enemy.setX(enemy.getX() + (enemy.getTargetX() - enemy.getX()) * Math.min(1, delta * 2.0));

        if (canCapturePlayer(enemy) && beamTouchesPlayer(enemy))
        {
            capturePlayer(enemy);
            return;
        }

        if (enemy.getTimer() <= 0)
        {
            beginReturn(enemy);
        }
    }

    private void updateReturn(Enemy enemy, double delta)
    {
        double homeX = getFormationX(enemy);
        double homeY = getFormationY(enemy);
        double dx = homeX - enemy.getX();
        double dy = homeY - enemy.getY();
        double distance = Math.hypot(dx, dy);
        double speed = GameConfig.ENEMY_RETURN_SPEED * (enemy.isCarryingCapturedShip() ? 0.86 : 1.0) * delta;

        if (distance <= speed || distance < 6)
        {
            enemy.setX(homeX);
            enemy.setY(homeY);
            enemy.setMode(EnemyMode.FORMATION);
            enemy.setTimer(0);
            return;
        }

        enemy.setX(enemy.getX() + dx / distance * speed);
        enemy.setY(enemy.getY() + dy / distance * speed);
    }

    private void updateBullets(double delta)
    {
        bullets.removeIf(bullet ->
        {
            bullet.update(delta);
            return bullet.getY() < GameConfig.PLAYFIELD_TOP - 24
                    || bullet.getY() > GameConfig.PLAYFIELD_BOTTOM + 24;
        });
    }

    private void updateRescueShip(double delta)
    {
        if (rescueShip == null)
        {
            return;
        }

        rescueShip.update(delta);
        if (rescueShip.getY() > GameConfig.PLAYFIELD_BOTTOM + 54)
        {
            rescueShip = null;
            return;
        }

        if (player != null
                && Math.abs(rescueShip.getX() - player.getX()) <= GameConfig.RESCUE_MAGNET_RADIUS
                && Math.abs(rescueShip.getY() - player.getY()) <= 34)
        {
            player.setDualFighter(true);
            player.setInvulnerable(1.0);
            score += 1000;
            explosions.add(new Explosion(player.getX() - 18, player.getY() - 6, 0.4));
            explosions.add(new Explosion(player.getX() + 18, player.getY() - 6, 0.4));
            rescueShip = null;
        }
    }

    private void resolveCollisions()
    {
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
                        boolean carrying = enemy.isCarryingCapturedShip();
                        enemy.kill();
                        explosions.add(new Explosion(ex, ey, 0.38));
                        score += getEnemyScore(enemy) + (enemy.getMode() == EnemyMode.FORMATION ? 0 : 80);
                        if (carrying)
                        {
                            releaseRescueShip(ex, ey + 16);
                            score += 600;
                        }
                        return true;
                    }
                }
            }
            else if (player != null && !player.isInvulnerable())
            {
                if (Math.abs(bullet.getX() - player.getX()) <= player.getCollisionHalfWidth()
                        && Math.abs(bullet.getY() - player.getY()) <= GameConfig.PLAYER_HEIGHT * 0.5)
                {
                    destroyPlayer();
                    return true;
                }
            }
            return false;
        });

        if (player == null || player.isInvulnerable())
        {
            return;
        }

        for (Enemy enemy : enemies)
        {
            if (!enemy.isAlive())
            {
                continue;
            }
            if (Math.abs(getEnemyX(enemy) - player.getX()) <= player.getCollisionHalfWidth() + GameConfig.ENEMY_WIDTH * 0.35
                    && Math.abs(getEnemyY(enemy) - player.getY()) <= GameConfig.PLAYER_HEIGHT * 0.45 + GameConfig.ENEMY_HEIGHT * 0.4)
            {
                enemy.kill();
                explosions.add(new Explosion(getEnemyX(enemy), getEnemyY(enemy), 0.34));
                destroyPlayer();
                return;
            }
        }
    }

    private void updateExplosions(double delta)
    {
        explosions.removeIf(explosion ->
        {
            explosion.update(delta);
            return explosion.isDone();
        });
    }

    private void destroyPlayer()
    {
        if (player == null)
        {
            return;
        }

        explosions.add(new Explosion(player.getX(), player.getY(), 0.62));
        if (player.isDualFighter())
        {
            explosions.add(new Explosion(player.getX() - 18, player.getY(), 0.46));
            explosions.add(new Explosion(player.getX() + 18, player.getY(), 0.46));
        }

        lives -= 1;
        if (lives <= 0)
        {
            player = null;
            status = Status.GAME_OVER;
            return;
        }

        spawnPlayer();
    }

    private void capturePlayer(Enemy enemy)
    {
        if (!canCapturePlayer(enemy))
        {
            return;
        }

        enemy.setCarryingCapturedShip(true);
        beginReturn(enemy);

        lives -= 1;
        if (lives <= 0)
        {
            player = null;
            status = Status.GAME_OVER;
            return;
        }

        spawnPlayer();
    }

    private boolean canCapturePlayer(Enemy enemy)
    {
        return player != null
                && !player.isInvulnerable()
                && !player.isDualFighter()
                && !enemy.isCarryingCapturedShip();
    }

    private boolean beamTouchesPlayer(Enemy enemy)
    {
        double beamHalfWidth = GameConfig.CAPTURE_BEAM_WIDTH * 0.5;
        double beamBottom = player.getY() + GameConfig.PLAYER_HEIGHT;
        return Math.abs(player.getX() - enemy.getX()) <= beamHalfWidth
                && beamBottom >= enemy.getY() + GameConfig.ENEMY_HEIGHT * 0.5;
    }

    private void releaseRescueShip(double x, double y)
    {
        rescueShip = new RescueShip(x, y, 112);
    }

    private void spawnPlayer()
    {
        player = new PlayerShip((GameConfig.PLAYFIELD_LEFT + GameConfig.PLAYFIELD_RIGHT) * 0.5,
                GameConfig.PLAYFIELD_BOTTOM - 8);
        player.setInvulnerable(GameConfig.PLAYER_INVULNERABLE_TIME);
        fireCooldown = 0;
    }

    private void spawnWave()
    {
        enemies.clear();
        bullets.removeIf(bullet -> !bullet.isFromPlayer());
        rescueShip = null;
        formationOffsetX = 0;
        formationOffsetY = 0;
        formationDirection = random.nextBoolean() ? 1 : -1;
        enemyFireCooldown = 1.0;
        diveCooldown = 1.5;

        for (int row = 0; row < GameConfig.ENEMY_ROWS; row++)
        {
            for (int col = 0; col < GameConfig.ENEMY_COLS; col++)
            {
                enemies.add(new Enemy(row, col, kindForRow(row)));
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
            double speed = 18 + random.nextDouble() * 88;
            double radius = 0.8 + random.nextDouble() * 1.8;
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

    private Enemy chooseShooter()
    {
        List<Enemy> candidates = new ArrayList<>();
        for (Enemy enemy : enemies)
        {
            if (!enemy.isAlive())
            {
                continue;
            }
            if (enemy.getMode() == EnemyMode.CAPTURE_BEAM)
            {
                continue;
            }
            candidates.add(enemy);
        }
        if (candidates.isEmpty())
        {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private boolean tryStartCaptureDive()
    {
        if (player == null || player.isDualFighter() || rescueShip != null || hasCapturedShipInPlay())
        {
            return false;
        }

        List<Enemy> candidates = new ArrayList<>();
        for (Enemy enemy : enemies)
        {
            if (enemy.isAlive() && enemy.getKind() == EnemyKind.BOSS && enemy.getMode() == EnemyMode.FORMATION)
            {
                candidates.add(enemy);
            }
        }

        if (candidates.isEmpty() || random.nextDouble() > Math.min(0.55, 0.24 + level * 0.04))
        {
            return false;
        }

        Enemy enemy = candidates.get(random.nextInt(candidates.size()));
        launchEnemy(enemy, EnemyMode.CAPTURE_APPROACH);
        enemy.setTargetX(clamp(player.getX(),
                GameConfig.PLAYFIELD_LEFT + GameConfig.CAPTURE_BEAM_WIDTH * 0.5,
                GameConfig.PLAYFIELD_RIGHT - GameConfig.CAPTURE_BEAM_WIDTH * 0.5));
        return true;
    }

    private boolean tryStartDive()
    {
        List<Enemy> candidates = new ArrayList<>();
        for (Enemy enemy : enemies)
        {
            if (enemy.isAlive() && enemy.getMode() == EnemyMode.FORMATION)
            {
                candidates.add(enemy);
            }
        }

        if (candidates.isEmpty())
        {
            return false;
        }

        Enemy enemy = candidates.get(random.nextInt(candidates.size()));
        launchEnemy(enemy, EnemyMode.DIVE);
        return true;
    }

    private void launchEnemy(Enemy enemy, EnemyMode mode)
    {
        enemy.setMode(mode);
        enemy.setX(getFormationX(enemy));
        enemy.setY(getFormationY(enemy));
        enemy.setTimer(0);
        enemy.setDiveDirection(player != null && player.getX() >= enemy.getX() ? 1 : -1);
    }

    private void beginReturn(Enemy enemy)
    {
        enemy.setMode(EnemyMode.RETURNING);
        enemy.setTimer(0);
    }

    private void checkEnemyPressure()
    {
        if (player == null)
        {
            return;
        }

        double dangerLine = player.getY() - GameConfig.PLAYER_HEIGHT * 0.4;
        for (Enemy enemy : enemies)
        {
            if (!enemy.isAlive())
            {
                continue;
            }
            if (enemy.getMode() != EnemyMode.FORMATION)
            {
                continue;
            }
            if (getEnemyY(enemy) + GameConfig.ENEMY_HEIGHT * 0.5 >= dangerLine)
            {
                status = Status.GAME_OVER;
                player = null;
                return;
            }
        }
    }

    private boolean hasCapturedShipInPlay()
    {
        if (rescueShip != null)
        {
            return true;
        }
        for (Enemy enemy : enemies)
        {
            if (enemy.isAlive() && enemy.isCarryingCapturedShip())
            {
                return true;
            }
        }
        return false;
    }

    private int countPlayerBullets()
    {
        int count = 0;
        for (Bullet bullet : bullets)
        {
            if (bullet.isFromPlayer())
            {
                count += 1;
            }
        }
        return count;
    }

    private EnemyKind kindForRow(int row)
    {
        if (row == 0)
        {
            return EnemyKind.BOSS;
        }
        if (row <= 2)
        {
            return EnemyKind.ESCORT;
        }
        return EnemyKind.GRUNT;
    }

    private int getEnemyScore(Enemy enemy)
    {
        return switch (enemy.getKind())
        {
        case BOSS -> 400;
        case ESCORT -> 160;
        case GRUNT -> 100;
        };
    }

    private double clamp(double value, double min, double max)
    {
        return Math.max(min, Math.min(max, value));
    }

    private double getFormationX(Enemy enemy)
    {
        double formationWidth = GameConfig.ENEMY_COLS * GameConfig.ENEMY_WIDTH
                + (GameConfig.ENEMY_COLS - 1) * GameConfig.ENEMY_HORIZONTAL_GAP;
        double startX = (GameConfig.PLAYFIELD_LEFT + GameConfig.PLAYFIELD_RIGHT - formationWidth) * 0.5
                + GameConfig.ENEMY_WIDTH * 0.5;
        return startX + enemy.getCol() * (GameConfig.ENEMY_WIDTH + GameConfig.ENEMY_HORIZONTAL_GAP) + formationOffsetX;
    }

    private double getFormationY(Enemy enemy)
    {
        double startY = GameConfig.PLAYFIELD_TOP + 30;
        return startY + enemy.getRow() * (GameConfig.ENEMY_HEIGHT + GameConfig.ENEMY_VERTICAL_GAP) + formationOffsetY;
    }

    public double getEnemyX(Enemy enemy)
    {
        return enemy.getMode() == EnemyMode.FORMATION ? getFormationX(enemy) : enemy.getX();
    }

    public double getEnemyY(Enemy enemy)
    {
        return enemy.getMode() == EnemyMode.FORMATION ? getFormationY(enemy) : enemy.getY();
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

    public RescueShip getRescueShip()
    {
        return rescueShip;
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

    public boolean hasCapturedShipInBoss()
    {
        for (Enemy enemy : enemies)
        {
            if (enemy.isAlive() && enemy.isCarryingCapturedShip())
            {
                return true;
            }
        }
        return false;
    }
}
