package com.codexaga;

public final class Bullet
{
    private double x;
    private double y;
    private final double velocity;
    private final boolean fromPlayer;

    public Bullet(double x, double y, double velocity, boolean fromPlayer)
    {
        this.x = x;
        this.y = y;
        this.velocity = velocity;
        this.fromPlayer = fromPlayer;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public boolean isFromPlayer()
    {
        return fromPlayer;
    }

    public void update(double delta)
    {
        y += velocity * delta;
    }
}
