package com.codexaga;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Random;

public final class CodexagaApp extends Application
{
    private final InputState input = new InputState();
    private GameState gameState;
    private Renderer renderer;

    @Override
    public void start(Stage stage)
    {
        Canvas canvas = new Canvas(GameConfig.WIDTH, GameConfig.HEIGHT);
        canvas.setFocusTraversable(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gameState = new GameState(new Random());
        renderer = new Renderer();

        Scene scene = new Scene(new StackPane(canvas));
        bindInput(scene);

        stage.setTitle("Codexaga");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        canvas.requestFocus();

        AnimationTimer timer = new AnimationTimer()
        {
            private long last;

            @Override
            public void handle(long now)
            {
                if (last == 0)
                {
                    last = now;
                    return;
                }
                double delta = (now - last) / 1_000_000_000.0;
                last = now;

                gameState.update(delta, input);
                renderer.render(gc, gameState);
            }
        };
        timer.start();
    }

    private void bindInput(Scene scene)
    {
        scene.setOnKeyPressed(event ->
        {
            KeyCode code = event.getCode();
            switch (code)
            {
            case LEFT, A -> input.setLeft(true);
            case RIGHT, D -> input.setRight(true);
            case SPACE, Z, X, UP -> input.setFire(true);
            case ENTER -> input.requestStart();
            case P -> input.requestPause();
            case R -> input.requestRestart();
            default -> {
            }
            }
        });

        scene.setOnKeyReleased(event ->
        {
            KeyCode code = event.getCode();
            switch (code)
            {
            case LEFT, A -> input.setLeft(false);
            case RIGHT, D -> input.setRight(false);
            case SPACE, Z, X, UP -> input.setFire(false);
            default -> {
            }
            }
        });
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
