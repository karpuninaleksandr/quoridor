package ru.ac.uniyar.model.players;

import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.algorithms.AlphaBetaAlgorithm;
import ru.ac.uniyar.model.algorithms.MinimaxAlgorithm;
import ru.ac.uniyar.model.algorithms.MonteCarloAlgorithm;
import ru.ac.uniyar.model.algorithms.RandomAlgorithm;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;

@Service
public class ComputerPlayerFabric {
    public ComputerPlayer getComputerPlayer(String typeOfPlayer, String hardnessLevel) {
        ComputerPlayer computerPlayer = new ComputerPlayer();
        computerPlayer.setAlgorithm(switch (ComputerAlgorithmType.findByDescription(typeOfPlayer)) {
            case RANDOM -> new RandomAlgorithm();
            case MINIMAX -> new MinimaxAlgorithm();
            case MONTECARLO -> new MonteCarloAlgorithm();
            case ALPHABETA -> new AlphaBetaAlgorithm();
        });
        computerPlayer.setHardnessLevel(ComputerPlayerHardnessLevel.findByDescription(hardnessLevel));
        return computerPlayer;
    }
}
