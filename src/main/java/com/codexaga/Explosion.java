package com.codexaga;

public final class Explosion
{
    private final double x;
    private final double y;
    private double timer;

    public Explosion(double x, double y, double duration)
    {
        this.x = x;
        this.y = y;
        this.timer = duration;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public double getTimer()
    {
        return timer;
    }

    public void update(double delta)
    {
        timer = Math.max(0, timer - delta);
    }

    public boolean isDone()
    {
        return timer <= 0;
    }
}
