/*
 * Copyright (c) 2009 - 2022, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.sumatra.snapshot;


import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.IVector3;
import edu.tigers.sumatra.referee.proto.SslGcRefereeMessage;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;


/**
 * A snapshot of a game situation.
 */
@Value
@Builder(toBuilder = true)
public class Snapshot
{
	@Singular
	Map<BotID, SnapObject> bots;
	SnapObject ball;
	SslGcRefereeMessage.Referee.Command command;
	SslGcRefereeMessage.Referee.Stage stage;
	@Builder.Default
	boolean autoContinue = true;
	IVector2 placementPos;
	@Singular
	Map<BotID, IVector3> moveDestinations;


	/**
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJSON()
	{
		JSONObject obj = new JSONObject();
		obj.put("bots", JsonConverter.encode(bots));
		obj.put("ball", ball.toJSON());
		if (command != null)
		{
			obj.put("command", JsonConverter.encode(command));
		}
		if (stage != null)
		{
			obj.put("stage", JsonConverter.encode(stage));
		}
		if (placementPos != null)
		{
			obj.put("placementPos", JsonConverter.encode(placementPos));
		}
		if (moveDestinations != null && !moveDestinations.isEmpty())
		{
			obj.put("moveDestinations", JsonConverter.encodeVector3Map(moveDestinations));
		}
		return obj;
	}


	/**
	 * @param obj
	 * @return
	 */
	public static Snapshot fromJSON(final JSONObject obj)
	{
		Map<BotID, SnapObject> bots = JsonConverter.decodeBots((JSONArray) obj.get("bots"));
		SnapObject ball = SnapObject.fromJSON((JSONObject) obj.get("ball"));
		SslGcRefereeMessage.Referee.Command command = Optional.ofNullable((String) obj.get("command"))
				.map(SslGcRefereeMessage.Referee.Command::valueOf).orElse(null);
		SslGcRefereeMessage.Referee.Stage stage = Optional.ofNullable((String) obj.get("stage"))
				.map(SslGcRefereeMessage.Referee.Stage::valueOf).orElse(null);
		IVector2 placementPos = Optional.ofNullable((JSONArray) obj.get("placementPos"))
				.map(JsonConverter::decodeVector2)
				.orElse(null);
		Map<BotID, IVector3> moveDestinations = JsonConverter.decodeVector3Map((JSONArray) obj.get("moveDestinations"));
		return Snapshot.builder()
				.bots(bots)
				.ball(ball)
				.command(command)
				.stage(stage)
				.placementPos(placementPos)
				.moveDestinations(moveDestinations)
				.build();
	}


	/**
	 * @param json
	 * @return
	 * @throws ParseException
	 */
	public static Snapshot fromJSONString(final String json) throws ParseException
	{
		JSONParser parser = new JSONParser();
		return Snapshot.fromJSON((JSONObject) parser.parse(json));
	}


	/**
	 * @param path to the snapshot file
	 * @return the loaded snapshot
	 * @throws IOException on any error
	 */
	public static Snapshot loadFromFile(final Path path) throws IOException
	{
		byte[] encoded = Files.readAllBytes(path);
		String json = new String(encoded, StandardCharsets.UTF_8);

		try
		{
			return Snapshot.fromJSONString(json);
		} catch (ParseException err)
		{
			throw new IOException("Could not parse snapshot.", err);
		}
	}


	/**
	 * Load a snapshot from a resource file
	 *
	 * @param filename name/path to the file in classpath
	 * @return the snapshot
	 * @throws IOException on any error
	 */
	public static Snapshot loadFromResources(final String filename)
			throws IOException
	{
		Path path;
		try
		{
			URL url = Snapshot.class.getClassLoader().getResource(filename);
			if (url == null)
			{
				throw new IOException("Resource not found: " + filename);
			}
			path = Paths.get(url.toURI());
		} catch (URISyntaxException e)
		{
			throw new IOException("Could not get path to resource file.", e);
		}
		return loadFromFile(path);
	}


	/**
	 * @param target
	 * @throws IOException
	 */
	public void save(final Path target) throws IOException
	{
		Files.createDirectories(target.toAbsolutePath().getParent());

		Files.writeString(
				target,
				toJSON().toJSONString(),
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE
		);
	}
}
