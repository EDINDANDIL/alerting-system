# Cleanup and verification script

$files = @(
    "parser/src/main/java/ru/models/Trader.java",
    "parser/src/main/java/ru/models/AbstractTrader.java",
    "parser/src/main/java/ru/models/TraderType.java",
    "parser/src/main/java/ru/models/AgentRegistry.java",
    "parser/src/main/java/ru/models/AgentRegistryImpl.java",
    "parser/src/main/java/ru/models/factories/TraderFactory.java",
    "parser/src/main/java/ru/models/agents/FundamentalTrader.java",
    "parser/src/main/java/ru/models/agents/MarketMaker.java",
    "parser/src/main/java/ru/models/agents/MomentumTrader.java",
    "parser/src/main/java/ru/models/agents/NoiseTrader.java",
    "parser/src/main/java/ru/models/Exchange.java",
    "parser/src/main/java/ru/models/OrderBook.java",
    "parser/src/main/java/ru/models/Order.java",
    "parser/src/main/java/ru/models/Side.java",
    "parser/src/main/java/ru/models/Type.java",
    "parser/src/main/java/ru/service/SimulationEngine.java",
    "parser/src/main/java/ru/models/SimulationContext.java",
    "parser/src/test/java/ru/models/AgentRegistryImplTest.java",
    "parser/src/test/java/ru/models/agents/FundamentalTraderTest.java",
    "parser/src/test/java/ru/models/agents/MarketMakerTest.java",
    "parser/src/test/java/ru/models/agents/MomentumTraderTest.java",
    "parser/src/test/java/ru/models/agents/NoiseTraderTest.java",
    "parser/src/test/java/ru/service/SimulationEngineTest.java"
)

$directories = @(
    "parser/src/main/java/ru/models/agents",
    "parser/src/main/java/ru/models/factories",
    "parser/src/main/java/ru/models",
    "parser/src/test/java/ru/models/agents",
    "parser/src/test/java/ru/models"
)

Write-Host "Starting file deletion..."
foreach ($file in $files) {
    if (Test-Path $file) {
        Remove-Item -Path $file -Force
        Write-Host "Deleted file: $file"
    } else {
        Write-Host "File already absent: $file"
    }
}

Write-Host "Starting empty directory cleanup..."
foreach ($dir in $directories) {
    if (Test-Path $dir) {
        Remove-Item -Path $dir -Recurse -Force
        Write-Host "Deleted directory: $dir"
    } else {
        Write-Host "Directory already absent: $dir"
    }
}

Write-Host "Running Gradle compilation..."
.\gradlew.bat compileJava --no-daemon

Write-Host "Running Gradle tests..."
.\gradlew.bat test --no-daemon
