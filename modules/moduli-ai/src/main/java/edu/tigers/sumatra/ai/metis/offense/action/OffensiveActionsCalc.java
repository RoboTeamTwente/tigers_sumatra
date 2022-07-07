/*
 * Copyright (c) 2009 - 2021, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ai.metis.offense.action;

import com.github.g3force.configurable.ConfigRegistration;
import com.github.g3force.configurable.Configurable;
import edu.tigers.sumatra.ai.metis.ACalculator;
import edu.tigers.sumatra.ai.metis.EAiShapesLayer;
import edu.tigers.sumatra.ai.metis.botdistance.BotDistance;
import edu.tigers.sumatra.ai.metis.offense.OffensiveConstants;
import edu.tigers.sumatra.ai.metis.offense.action.moves.AOffensiveActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.ClearingKickActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.EOffensiveActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.FinisherActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.ForcedPassActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.GoalKickActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.KickInsBlaueActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.LowChanceGoalKickActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.MoveBallToOpponentHalfActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.ProtectActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.ReceiveBallActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.RedirectGoalKickActionMove;
import edu.tigers.sumatra.ai.metis.offense.action.moves.StandardPassActionMove;
import edu.tigers.sumatra.ai.metis.offense.dribble.DribblingInformation;
import edu.tigers.sumatra.ai.metis.offense.statistics.OffensiveStatisticsFrame;
import edu.tigers.sumatra.ai.metis.offense.strategy.EOffensiveStrategy;
import edu.tigers.sumatra.ai.metis.pass.KickOrigin;
import edu.tigers.sumatra.ai.metis.pass.rating.RatedPass;
import edu.tigers.sumatra.ai.metis.targetrater.GoalKick;
import edu.tigers.sumatra.drawable.DrawableAnnotation;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.math.circle.ICircle;
import edu.tigers.sumatra.math.vector.Vector2f;
import edu.tigers.sumatra.trees.EOffensiveSituation;
import edu.tigers.sumatra.trees.OffensiveActionTree;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Calculates offensive Actions for the OffenseRole.
 */
@Log4j2
@RequiredArgsConstructor
public class OffensiveActionsCalc extends ACalculator
{
	private static final DecimalFormat DF = new DecimalFormat("0.00");
	private static final Color COLOR = Color.magenta;

	@Configurable(defValue = "0.25")
	private static double viabilityMultiplierClearingKick = 0.25;

	@Configurable(defValue = "1.0")
	private static double viabilityMultiplierDirectKick = 1.0;

	@Configurable(defValue = "0.2")
	private static double viabilityMultiplierKickInsBlaue = 0.2;

	@Configurable(defValue = "0.7")
	private static double viabilityMultiplierStandardPass = 0.7;

	@Configurable(defValue = "1.0")
	private static double viabilityMultiplierGoToOtherHalf = 1.0;

	@Configurable(defValue = "0.4")
	private static double impactTimeActionLockThreshold = 0.4;

	@Configurable(defValue = "PROTECT_MOVE")
	private static EOffensiveActionMove forcedOffensiveActionMove = EOffensiveActionMove.PROTECT_MOVE;

	@Configurable(comment = "Forces OffensiveActionsCalc to always activate the actionMove configured in forcedOffensiveActionMove", defValue = "false")
	private static boolean activatedForcedOffensiveMove = false;

	static
	{
		for (EOffensiveActionMove actionMove : EOffensiveActionMove.values())
		{
			ConfigRegistration.registerClass("metis", actionMove.getInstanceableClass().getImpl());
		}
	}

	private final Supplier<EOffensiveStrategy> ballHandlingRobotsStrategy;
	private final Supplier<List<BotID>> ballHandlingBots;
	private final Supplier<Map<KickOrigin, RatedPass>> selectedPasses;
	private final Supplier<Map<BotID, KickOrigin>> kickOrigins;
	private final Supplier<BotDistance> opponentClosestToBall;
	private final Supplier<OffensiveStatisticsFrame> offensiveStatisticsFrameSupplier;
	private final Supplier<Map<BotID, GoalKick>> bestGoalKickPerBot;
	private final Supplier<List<ICircle>> kickInsBlaueSpots;
	private final Supplier<DribblingInformation> dribblingInformation;
	private final Map<EOffensiveActionMove, AOffensiveActionMove> actionMoves = new EnumMap<>(
			EOffensiveActionMove.class);
	@Getter
	private Map<BotID, OffensiveAction> offensiveActions;


	@Override
	protected void start()
	{
		register(EOffensiveActionMove.FORCED_PASS, new ForcedPassActionMove(
				selectedPasses
		));
		register(EOffensiveActionMove.REDIRECT_GOAL_KICK, new RedirectGoalKickActionMove(
				bestGoalKickPerBot
		));
		register(EOffensiveActionMove.FINISHER, new FinisherActionMove(
				opponentClosestToBall,
				dribblingInformation
		));
		register(EOffensiveActionMove.GOAL_KICK, new GoalKickActionMove(
				bestGoalKickPerBot
		));
		register(EOffensiveActionMove.CLEARING_KICK, new ClearingKickActionMove(
				opponentClosestToBall,
				bestGoalKickPerBot,
				kickOrigins
		));
		register(EOffensiveActionMove.STANDARD_PASS, new StandardPassActionMove(
				selectedPasses
		));
		register(EOffensiveActionMove.LOW_CHANCE_GOAL_KICK, new LowChanceGoalKickActionMove(
				bestGoalKickPerBot
		));
		register(EOffensiveActionMove.MOVE_BALL_TO_OPPONENT_HALF, new MoveBallToOpponentHalfActionMove(
				opponentClosestToBall
		));
		register(EOffensiveActionMove.KICK_INS_BLAUE, new KickInsBlaueActionMove(
				kickInsBlaueSpots
		));
		register(EOffensiveActionMove.RECEIVE_BALL, new ReceiveBallActionMove(
				selectedPasses,
				kickOrigins
		));
		register(EOffensiveActionMove.PROTECT_MOVE, new ProtectActionMove(
				kickOrigins,
				dribblingInformation
		));
	}


	private void register(EOffensiveActionMove type, AOffensiveActionMove move)
	{
		actionMoves.put(type, move);
	}


	@Override
	protected void stop()
	{
		actionMoves.clear();
	}


	@Override
	protected boolean isCalculationNecessary()
	{
		return ballHandlingRobotsStrategy.get() == EOffensiveStrategy.KICK;
	}


	@Override
	protected void reset()
	{
		offensiveActions = Collections.emptyMap();
	}


	@Override
	public void doCalc()
	{
		var adjustedScores = calcAdjustedOffensiveViabilityScores();
		var scoreMultipliers = calcScoreMultipliers(adjustedScores);
		actionMoves.values().forEach(m -> m.update(getAiFrame()));
		actionMoves.forEach((moveType, actionMove) -> actionMove.setScoreMultiplier(scoreMultipliers.get(moveType)));

		Map<BotID, OffensiveAction> newOffensiveActions = new HashMap<>();
		for (var botId : ballHandlingBots.get())
		{
			var bestAction = findOffensiveActions(botId);
			newOffensiveActions.put(botId, bestAction);
			drawAction(botId, bestAction);
		}
		offensiveActions = Collections.unmodifiableMap(newOffensiveActions);
	}


	private OffensiveAction findOffensiveActions(BotID botId)
	{
		var kickOrigin = kickOrigins.get().get(botId);
		if (kickOrigin != null)
		{
			if (kickOrigin.getImpactTime() < impactTimeActionLockThreshold
					&& offensiveActions.containsKey(botId)
					&& offensiveActions.get(botId).getMove() != EOffensiveActionMove.PROTECT_MOVE)
			{
				return getStableOffensiveAction(botId);
			}

			getShapes(EAiShapesLayer.OFFENSIVE_ACTION_DEBUG)
					.add(getActionUpdateDebugAnnotation(botId, "accepting new action"));

			if (activatedForcedOffensiveMove)
			{
				return actionMoves.get(forcedOffensiveActionMove).calcAction(botId);
			}

			var actions = actionMoves.values().stream().map(m -> m.calcAction(botId)).collect(Collectors.toList());
			drawViabilityMap(botId, actions);

			var bestAction = bestFullyViableAction(actions).or(() -> bestPartiallyViableAction(actions)).orElse(null);
			if (bestAction != null)
			{
				if (OffensiveConstants.isEnableOffensiveStatistics())
				{
					var offensiveStatisticsFrame = offensiveStatisticsFrameSupplier.get();
					var viabilityMap = actions.stream()
							.collect(Collectors.toMap(OffensiveAction::getMove, OffensiveAction::getViability));
					var offensiveBotFrame = offensiveStatisticsFrame.getBotFrames().get(botId);
					offensiveBotFrame.setMoveViabilityMap(viabilityMap);
				}

				return bestAction;
			}
			log.warn("No offensive action has been declared... desperation level > 9000");
		}

		// fallback in case that no action could be found (by intention or by accident)
		// This will happen if there is no kick origin and thus no known point to intercept the ball,
		// so we must first get control of the ball again
		if (getBall().getVel().getLength2() > 0.5)
		{
			return actionMoves.get(EOffensiveActionMove.RECEIVE_BALL).calcAction(botId);
		}
		return actionMoves.get(EOffensiveActionMove.LOW_CHANCE_GOAL_KICK).calcAction(botId);
	}


	private OffensiveAction getStableOffensiveAction(BotID botId)
	{
		var lastActionMove = offensiveActions.get(botId).getMove();
		var updatedAction = actionMoves.get(lastActionMove).calcAction(botId);
		var viability = updatedAction.getViability();

		if (viability.getType() == EActionViability.FALSE)
		{
			// keep old action, since an update is not possible
			getShapes(EAiShapesLayer.OFFENSIVE_ACTION_DEBUG)
					.add(getActionUpdateDebugAnnotation(botId, "keeping old action"));
			return offensiveActions.get(botId);
		}

		// keep the current action, but keep updating it
		getShapes(EAiShapesLayer.OFFENSIVE_ACTION_DEBUG)
				.add(getActionUpdateDebugAnnotation(botId, "updating old action"));
		return updatedAction;
	}


	private DrawableAnnotation getActionUpdateDebugAnnotation(BotID botId, String text)
	{
		return new DrawableAnnotation(getWFrame().getBot(botId).getPos(), text)
				.withOffset(Vector2f.fromXY(-100, 350)).setColor(COLOR);
	}


	private void drawViabilityMap(BotID botID, List<OffensiveAction> actions)
	{
		var pos = getWFrame().getBot(botID).getPos();
		var text = actions.stream()
				.map(this::drawViability)
				.collect(Collectors.joining("\n"));
		getShapes(EAiShapesLayer.OFFENSIVE_ACTION_DEBUG)
				.add(new DrawableAnnotation(pos, text).withOffset(Vector2f.fromX(250)).setColor(COLOR));
	}


	private String drawViability(OffensiveAction v)
	{
		var viabilityType = v.getViability().getType().name().charAt(0);
		var score = DF.format(v.getViability().getScore());
		return viabilityType + score + ":" + v.getMove();
	}


	private Map<EOffensiveActionMove, Double> calcScoreMultipliers(Map<String, Double> adjustedScores)
	{
		Map<EOffensiveActionMove, Double> multiplierMap = new EnumMap<>(EOffensiveActionMove.class);
		// Fixed default multipliers
		multiplierMap.put(EOffensiveActionMove.CLEARING_KICK, viabilityMultiplierClearingKick);
		multiplierMap.put(EOffensiveActionMove.GOAL_KICK, viabilityMultiplierDirectKick);
		multiplierMap.put(EOffensiveActionMove.FINISHER, viabilityMultiplierDirectKick);
		multiplierMap.put(EOffensiveActionMove.LOW_CHANCE_GOAL_KICK, viabilityMultiplierDirectKick);
		multiplierMap.put(EOffensiveActionMove.KICK_INS_BLAUE, viabilityMultiplierKickInsBlaue);
		multiplierMap.put(EOffensiveActionMove.STANDARD_PASS, viabilityMultiplierStandardPass);
		multiplierMap.put(EOffensiveActionMove.MOVE_BALL_TO_OPPONENT_HALF, viabilityMultiplierGoToOtherHalf);

		Arrays.stream(EOffensiveActionMove.values()).forEach(v -> {
			// Set remaining values to 1.0
			multiplierMap.putIfAbsent(v, 1.0);
			// Apply the adjusted score to each action move
			multiplierMap.computeIfPresent(v, (move, score) -> score * adjustedScores.getOrDefault(v.name(), 1.0));
		});
		return multiplierMap;
	}


	private void drawAction(BotID botID, OffensiveAction action)
	{
		var bot = getWFrame().getBot(botID);
		final String actionMetadata = action.getMove() + "\n";
		getShapes(EAiShapesLayer.OFFENSIVE_ACTION).add(
				new DrawableAnnotation(bot.getPos(), actionMetadata, COLOR)
						.withOffset(Vector2f.fromY(150))
						.withCenterHorizontally(true));
	}


	private Map<String, Double> calcAdjustedOffensiveViabilityScores()
	{
		var currentPath = getAiFrame().getPrevFrame().getTacticalField().getCurrentPath().getCurrentPath();
		Map<EOffensiveSituation, OffensiveActionTree> trees = getAiFrame().getPrevFrame().getTacticalField()
				.getActionTrees()
				.getActionTrees();
		var currentSituation = getAiFrame().getPrevFrame().getTacticalField().getCurrentSituation();

		Map<String, Double> adjustedScores = new HashMap<>();
		if (trees.containsKey(currentSituation))
		{
			var scoresFromTree = trees.get(currentSituation)
					.getAdjustedScoresForCurrentPath(
							currentPath.stream().map(Enum::name).toArray(String[]::new));
			scoresFromTree.forEach(adjustedScores::put);
		}
		for (EOffensiveActionMove key : EOffensiveActionMove.values())
		{
			adjustedScores.putIfAbsent(key.name(), 1.0);
		}
		return adjustedScores;
	}


	private Optional<OffensiveAction> bestFullyViableAction(List<OffensiveAction> actions)
	{
		return actions.stream()
				.filter(v -> v.getViability().getType() == EActionViability.TRUE)
				// this works because the EnumMap uses the natural ordering of the underlying Enum
				.findFirst();
	}


	private Optional<OffensiveAction> bestPartiallyViableAction(List<OffensiveAction> actions)
	{
		return actions.stream()
				.filter(v -> v.getViability().getType() == EActionViability.PARTIALLY)
				.max(Comparator.comparingDouble(m -> m.getViability().getScore()));
	}
}
