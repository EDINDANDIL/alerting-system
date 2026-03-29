package ru.alertcli.commands;

import picocli.CommandLine;

@CommandLine.Command(name = "exit", description = "Выйти из приложения", aliases = {"quit", "q"})
public class ExitCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Goodbye!");
        System.exit(0);
    }
}
