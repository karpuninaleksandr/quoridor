package ru.ac.uniyar.model.players;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.algorithms.AlphaBetaAlgorithm;
import ru.ac.uniyar.model.algorithms.MinimaxAlgorithm;
import ru.ac.uniyar.model.algorithms.MonteCarloAlgorithm;
import ru.ac.uniyar.model.algorithms.RandomAlgorithm;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;

import java.util.HashMap;
import java.util.Map;

@Service
public class ComputerPlayerFabric {
    private final Map<ComputerAlgorithmType, Map<ComputerPlayerHardnessLevel, ComputerPlayer>> players = new HashMap<>();

    public ComputerPlayer getComputerPlayer(String typeOfPlayer, String hardnessLevel) {
        return players.get(ComputerAlgorithmType.findByDescription(typeOfPlayer)).get(ComputerPlayerHardnessLevel.findByDescription(hardnessLevel));
    }

    @PostConstruct()
    private void createPlayers() {
        for (ComputerAlgorithmType computerAlgorithmType : ComputerAlgorithmType.values()) {
            players.put(computerAlgorithmType, new HashMap<>());
            for (ComputerPlayerHardnessLevel computerPlayerHardnessLevel : ComputerPlayerHardnessLevel.values()) {
                ComputerPlayer computerPlayer = new ComputerPlayer();
                computerPlayer.setAlgorithm(switch (computerAlgorithmType) {
                    case RANDOM -> new RandomAlgorithm();
                    case MINIMAX -> new MinimaxAlgorithm();
                    case MONTECARLO -> new MonteCarloAlgorithm();
                    case ALPHABETA -> new AlphaBetaAlgorithm();
                });
                computerPlayer.setHardnessLevel(computerPlayerHardnessLevel);
                players.get(computerAlgorithmType).put(computerPlayerHardnessLevel, computerPlayer);
            }
        }
    }
}
