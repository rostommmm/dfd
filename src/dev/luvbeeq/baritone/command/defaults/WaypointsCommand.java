package dev.luvbeeq.baritone.command.defaults;

import dev.luvbeeq.baritone.Baritone;
import dev.luvbeeq.baritone.api.IBaritone;
import dev.luvbeeq.baritone.api.cache.IWaypoint;
import dev.luvbeeq.baritone.api.cache.IWorldData;
import dev.luvbeeq.baritone.api.cache.Waypoint;
import dev.luvbeeq.baritone.api.command.Command;
import dev.luvbeeq.baritone.api.command.argument.IArgConsumer;
import dev.luvbeeq.baritone.api.command.datatypes.ForWaypoints;
import dev.luvbeeq.baritone.api.command.datatypes.RelativeBlockPos;
import dev.luvbeeq.baritone.api.command.exception.CommandException;
import dev.luvbeeq.baritone.api.command.exception.CommandInvalidStateException;
import dev.luvbeeq.baritone.api.command.exception.CommandInvalidTypeException;
import dev.luvbeeq.baritone.api.command.helpers.Paginator;
import dev.luvbeeq.baritone.api.command.helpers.TabCompleteHelper;
import dev.luvbeeq.baritone.api.pathing.goals.Goal;
import dev.luvbeeq.baritone.api.pathing.goals.GoalBlock;
import dev.luvbeeq.baritone.api.utils.BetterBlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.luvbeeq.baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class WaypointsCommand extends Command {

    private Map<IWorldData, List<IWaypoint>> deletedWaypoints = new HashMap<>();

    public WaypointsCommand(IBaritone baritone) {
        super(baritone, "waypoints", "waypoint", "wp");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        Action action = args.hasAny() ? Action.getByName(args.getString()) : Action.LIST;
        if (action == null) {
            throw new CommandInvalidTypeException(args.consumed(), "an action");
        }
        BiFunction<IWaypoint, Action, ITextComponent> toComponent = (waypoint, _action) -> {
            TextComponent component = new StringTextComponent("");
            TextComponent tagComponent = new StringTextComponent(waypoint.getTag().name() + " ");
            tagComponent.setStyle(tagComponent.getStyle().setFormatting(TextFormatting.GRAY));
            String name = waypoint.getName();
            TextComponent nameComponent = new StringTextComponent(!name.isEmpty() ? name : "<empty>");
            nameComponent.setStyle(nameComponent.getStyle().setFormatting(!name.isEmpty() ? TextFormatting.GRAY : TextFormatting.DARK_GRAY));
            TextComponent timestamp = new StringTextComponent(" @ " + new Date(waypoint.getCreationTimestamp()));
            timestamp.setStyle(timestamp.getStyle().setFormatting(TextFormatting.DARK_GRAY));
            component.append(tagComponent);
            component.append(nameComponent);
            component.append(timestamp);
            component.setStyle(component.getStyle()
                    .setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("Click to select")
                    ))
                    .setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format(
                                    "%s%s %s %s @ %d",
                                    FORCE_COMMAND_PREFIX,
                                    label,
                                    _action.names[0],
                                    waypoint.getTag().getName(),
                                    waypoint.getCreationTimestamp()
                            ))
                    ));
            return component;
        };
        Function<IWaypoint, ITextComponent> transform = waypoint ->
                toComponent.apply(waypoint, action == Action.LIST ? Action.INFO : action);
        if (action == Action.LIST) {
            IWaypoint.Tag tag = args.hasAny() ? IWaypoint.Tag.getByName(args.peekString()) : null;
            if (tag != null) {
                args.get();
            }
            IWaypoint[] waypoints = tag != null
                    ? ForWaypoints.getWaypointsByTag(this.baritone, tag)
                    : ForWaypoints.getWaypoints(this.baritone);
            if (waypoints.length > 0) {
                args.requireMax(1);
                Paginator.paginate(
                        args,
                        waypoints,
                        () -> logDirect(
                                tag != null
                                        ? String.format("All waypoints by tag %s:", tag.name())
                                        : "All waypoints:"
                        ),
                        transform,
                        String.format(
                                "%s%s %s%s",
                                FORCE_COMMAND_PREFIX,
                                label,
                                action.names[0],
                                tag != null ? " " + tag.getName() : ""
                        )
                );
            } else {
                args.requireMax(0);
                throw new CommandInvalidStateException(
                        tag != null
                                ? "No waypoints found by that tag"
                                : "No waypoints found"
                );
            }
        } else if (action == Action.SAVE) {
            IWaypoint.Tag tag = args.hasAny() ? IWaypoint.Tag.getByName(args.peekString()) : null;
            if (tag == null) {
                tag = IWaypoint.Tag.USER;
            } else {
                args.get();
            }
            String name = (args.hasExactlyOne() || args.hasExactly(4)) ? args.getString() : "";
            BetterBlockPos pos = args.hasAny()
                    ? args.getDatatypePost(RelativeBlockPos.INSTANCE, ctx.playerFeet())
                    : ctx.playerFeet();
            args.requireMax(0);
            IWaypoint waypoint = new Waypoint(name, tag, pos);
            ForWaypoints.waypoints(this.baritone).addWaypoint(waypoint);
            TextComponent component = new StringTextComponent("Waypoint added: ");
            component.setStyle(component.getStyle().setFormatting(TextFormatting.GRAY));
            component.append(toComponent.apply(waypoint, Action.INFO));
            logDirect(component);
        } else if (action == Action.CLEAR) {
            args.requireMax(1);
            String name = args.getString();
            IWaypoint.Tag tag = IWaypoint.Tag.getByName(name);
            if (tag == null) {
                throw new CommandInvalidStateException("Invalid tag, \"" + name + "\"");
            }
            IWaypoint[] waypoints = ForWaypoints.getWaypointsByTag(this.baritone, tag);
            for (IWaypoint waypoint : waypoints) {
                ForWaypoints.waypoints(this.baritone).removeWaypoint(waypoint);
            }
            deletedWaypoints.computeIfAbsent(baritone.getWorldProvider().getCurrentWorld(), k -> new ArrayList<>()).addAll(Arrays.<IWaypoint>asList(waypoints));
            TextComponent textComponent = new StringTextComponent(String.format("Cleared %d waypoints, click to restore them", waypoints.length));
            textComponent.setStyle(textComponent.getStyle().setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    String.format(
                            "%s%s restore @ %s",
                            FORCE_COMMAND_PREFIX,
                            label,
                            Stream.of(waypoints).map(wp -> Long.toString(wp.getCreationTimestamp())).collect(Collectors.joining(" "))
                    )
            )));
            logDirect(textComponent);
        } else if (action == Action.RESTORE) {
            List<IWaypoint> waypoints = new ArrayList<>();
            List<IWaypoint> deletedWaypoints = this.deletedWaypoints.getOrDefault(baritone.getWorldProvider().getCurrentWorld(), Collections.emptyList());
            if (args.peekString().equals("@")) {
                args.get();
                // no args.requireMin(1) because if the user clears an empty tag there is nothing to restore
                while (args.hasAny()) {
                    long timestamp = args.getAs(Long.class);
                    for (IWaypoint waypoint : deletedWaypoints) {
                        if (waypoint.getCreationTimestamp() == timestamp) {
                            waypoints.add(waypoint);
                            break;
                        }
                    }
                }
            } else {
                args.requireExactly(1);
                int size = deletedWaypoints.size();
                int amount = Math.min(size, args.getAs(Integer.class));
                waypoints = new ArrayList<>(deletedWaypoints.subList(size - amount, size));
            }
            waypoints.forEach(ForWaypoints.waypoints(this.baritone)::addWaypoint);
            deletedWaypoints.removeIf(waypoints::contains);
            logDirect(String.format("Restored %d waypoints", waypoints.size()));
        } else {
            IWaypoint[] waypoints = args.getDatatypeFor(ForWaypoints.INSTANCE);
            IWaypoint waypoint = null;
            if (args.hasAny() && args.peekString().equals("@")) {
                args.requireExactly(2);
                args.get();
                long timestamp = args.getAs(Long.class);
                for (IWaypoint iWaypoint : waypoints) {
                    if (iWaypoint.getCreationTimestamp() == timestamp) {
                        waypoint = iWaypoint;
                        break;
                    }
                }
                if (waypoint == null) {
                    throw new CommandInvalidStateException("Timestamp was specified but no waypoint was found");
                }
            } else {
                switch (waypoints.length) {
                    case 0:
                        throw new CommandInvalidStateException("No waypoints found");
                    case 1:
                        waypoint = waypoints[0];
                        break;
                    default:
                        break;
                }
            }
            if (waypoint == null) {
                args.requireMax(1);
                Paginator.paginate(
                        args,
                        waypoints,
                        () -> logDirect("Multiple waypoints were found:"),
                        transform,
                        String.format(
                                "%s%s %s %s",
                                FORCE_COMMAND_PREFIX,
                                label,
                                action.names[0],
                                args.consumedString()
                        )
                );
            } else {
                if (action == Action.INFO) {
                    logDirect(transform.apply(waypoint));
                    logDirect(String.format("Position: %s", waypoint.getLocation()));
                    TextComponent deleteComponent = new StringTextComponent("Click to delete this waypoint");
                    deleteComponent.setStyle(deleteComponent.getStyle().setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format(
                                    "%s%s delete %s @ %d",
                                    FORCE_COMMAND_PREFIX,
                                    label,
                                    waypoint.getTag().getName(),
                                    waypoint.getCreationTimestamp()
                            )
                    )));
                    TextComponent goalComponent = new StringTextComponent("Click to set goal to this waypoint");
                    goalComponent.setStyle(goalComponent.getStyle().setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format(
                                    "%s%s goal %s @ %d",
                                    FORCE_COMMAND_PREFIX,
                                    label,
                                    waypoint.getTag().getName(),
                                    waypoint.getCreationTimestamp()
                            )
                    )));
                    TextComponent recreateComponent = new StringTextComponent("Click to show a command to recreate this waypoint");
                    recreateComponent.setStyle(recreateComponent.getStyle().setClickEvent(new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            String.format(
                                    "%s%s save %s %s %s %s %s",
                                    Baritone.settings().prefix.value, // This uses the normal prefix because it is run by the user.
                                    label,
                                    waypoint.getTag().getName(),
                                    waypoint.getName(),
                                    waypoint.getLocation().x,
                                    waypoint.getLocation().y,
                                    waypoint.getLocation().z
                            )
                    )));
                    TextComponent backComponent = new StringTextComponent("Click to return to the waypoints list");
                    backComponent.setStyle(backComponent.getStyle().setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format(
                                    "%s%s list",
                                    FORCE_COMMAND_PREFIX,
                                    label
                            )
                    )));
                    logDirect(deleteComponent);
                    logDirect(goalComponent);
                    logDirect(recreateComponent);
                    logDirect(backComponent);
                } else if (action == Action.DELETE) {
                    ForWaypoints.waypoints(this.baritone).removeWaypoint(waypoint);
                    deletedWaypoints.computeIfAbsent(baritone.getWorldProvider().getCurrentWorld(), k -> new ArrayList<>()).add(waypoint);
                    TextComponent textComponent = new StringTextComponent("That waypoint has successfully been deleted, click to restore it");
                    textComponent.setStyle(textComponent.getStyle().setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format(
                                    "%s%s restore @ %s",
                                    FORCE_COMMAND_PREFIX,
                                    label,
                                    waypoint.getCreationTimestamp()
                            )
                    )));
                    logDirect(textComponent);
                } else if (action == Action.GOAL) {
                    Goal goal = new GoalBlock(waypoint.getLocation());
                    baritone.getCustomGoalProcess().setGoal(goal);
                    logDirect(String.format("Goal: %s", goal));
                } else if (action == Action.GOTO) {
                    Goal goal = new GoalBlock(waypoint.getLocation());
                    baritone.getCustomGoalProcess().setGoalAndPath(goal);
                    logDirect(String.format("Going to: %s", goal));
                }
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            if (args.hasExactlyOne()) {
                return new TabCompleteHelper()
                        .append(Action.getAllNames())
                        .sortAlphabetically()
                        .filterPrefix(args.getString())
                        .stream();
            } else {
                Action action = Action.getByName(args.getString());
                if (args.hasExactlyOne()) {
                    if (action == Action.LIST || action == Action.SAVE || action == Action.CLEAR) {
                        return new TabCompleteHelper()
                                .append(IWaypoint.Tag.getAllNames())
                                .sortAlphabetically()
                                .filterPrefix(args.getString())
                                .stream();
                    } else if (action == Action.RESTORE) {
                        return Stream.empty();
                    } else {
                        return args.tabCompleteDatatype(ForWaypoints.INSTANCE);
                    }
                } else if (args.has(3) && action == Action.SAVE) {
                    args.get();
                    args.get();
                    return args.tabCompleteDatatype(RelativeBlockPos.INSTANCE);
                }
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Manage waypoints";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The waypoint command allows you to manage Baritone's waypoints.",
                "",
                "Waypoints can be used to mark positions for later. Waypoints are each given a tag and an optional name.",
                "",
                "Note that the info, delete, and goal commands let you specify a waypoint by tag. If there is more than one waypoint with a certain tag, then they will let you select which waypoint you mean.",
                "",
                "Missing arguments for the save command use the USER tag, creating an unnamed waypoint and your current position as defaults.",
                "",
                "Usage:",
                "> wp [l/list] - List all waypoints.",
                "> wp <l/list> <tag> - List all waypoints by tag.",
                "> wp <s/save> - Save an unnamed USER waypoint at your current position",
                "> wp <s/save> [tag] [name] [pos] - Save a waypoint with the specified tag, name and position.",
                "> wp <i/info/show> <tag/name> - Show info on a waypoint by tag or name.",
                "> wp <d/delete> <tag/name> - Delete a waypoint by tag or name.",
                "> wp <restore> <n> - Restore the last n deleted waypoints.",
                "> wp <c/clear> <tag> - Delete all waypoints with the specified tag.",
                "> wp <g/goal> <tag/name> - Set a goal to a waypoint by tag or name.",
                "> wp <goto> <tag/name> - Set a goal to a waypoint by tag or name and start pathing."
        );
    }

    private enum Action {
        LIST("list", "get", "l"),
        CLEAR("clear", "c"),
        SAVE("save", "s"),
        INFO("info", "show", "i"),
        DELETE("delete", "d"),
        RESTORE("restore"),
        GOAL("goal", "g"),
        GOTO("goto");
        private final String[] names;

        Action(String... names) {
            this.names = names;
        }

        public static Action getByName(String name) {
            for (Action action : Action.values()) {
                for (String alias : action.names) {
                    if (alias.equalsIgnoreCase(name)) {
                        return action;
                    }
                }
            }
            return null;
        }

        public static String[] getAllNames() {
            Set<String> names = new HashSet<>();
            for (Action action : Action.values()) {
                names.addAll(Arrays.asList(action.names));
            }
            return names.toArray(new String[0]);
        }
    }
}
