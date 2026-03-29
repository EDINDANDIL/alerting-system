package ru.alertcli.commands;

import picocli.CommandLine;

@CommandLine.Command(
    name = "filter",
    description = "Управление фильтрами (create, delete)",
    subcommands = {
        FilterCreateCommand.class,
        FilterDeleteCommand.class
    }
)
public class FilterCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use 'filter create' or 'filter delete'");
    }
}
