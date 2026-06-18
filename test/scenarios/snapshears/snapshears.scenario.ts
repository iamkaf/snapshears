import { Capability, Readiness, describe, expect, test } from "@teakit/test";
import type { ScenarioDefinition } from "@teakit/test";

describe.configure({
  timeout: "6m",
  readiness: [Readiness.ClientReady, Readiness.IntegratedServerReady, Readiness.PlayerSpawned],
  capabilities: [Capability.LegacyJsonScenarios, Capability.ServerCommands, Capability.ClientInput],
});

describe("SnapShears", () => {
  test("shears nearby sheep in a burst", async ({ scenario }) => {
    const result = await scenario.run(shearBurstDefinition, { timeoutMs: 240_000 });

    expect(result.error ?? null).toBeNull();
  });

  test("sneaking narrows a burst to the target sheep color", async ({ client, commands, scenario }) => {
    try {
      const result = await scenario.run(sneakingColorBurstDefinition, { timeoutMs: 240_000 });

      expect(result.error ?? null).toBeNull();
      const sheepData = await commands.run("/execute as @e[type=minecraft:sheep,distance=..8] run data get entity @s", {
        captureOutput: true,
      });
      const sheepLines = "output" in sheepData && Array.isArray(sheepData.output) ? sheepData.output : [];
      const shearedWhite = sheepLines.find((line) => line.includes("Color: 0b") && line.includes("Sheared: 1b"));
      const red = sheepLines.find((line) => line.includes("Color: 14b"));

      expect(shearedWhite).toBeTruthy();
      expect(red).toContain("Sheared: 0b");
    } finally {
      await client.keyState(340, false);
      await commands.run("/tp @s 0 70 0");
      await commands.run("/clear @s");
      await commands.run("/kill @e[type=minecraft:sheep,distance=..16]");
      await commands.run("/kill @e[type=minecraft:item,distance=..16]");
      await commands.run("/fill -2 69 0 2 69 5 minecraft:air replace");
      await commands.run("/fill -2 70 0 2 74 5 minecraft:air replace");
    }
  });
});

const shearBurstDefinition = {
  name: "snapshears-shear-burst",
  setup: [
    { action: "command", command: "/tp @s 0 70 0" },
    { action: "command", command: "/kill @e[type=minecraft:sheep,distance=..16]" },
    { action: "command", command: "/kill @e[type=minecraft:item,distance=..16]" },
    {
      action: "wait_for_entity_count",
      radius: 16,
      entityType: "minecraft:sheep",
      count: 0,
      timeoutMs: 5000,
    },
    {
      action: "wait_for_entity_count",
      radius: 16,
      entityType: "minecraft:item",
      count: 0,
      timeoutMs: 5000,
    },
    { action: "command", command: "/fill -2 69 0 2 69 5 minecraft:air replace" },
    { action: "command", command: "/fill -2 70 0 2 74 5 minecraft:air replace" },
    { action: "command", command: "/clear @s" },
    { action: "command", command: "/gamemode survival @s" },
    { action: "command", command: "/tp @s 0.5 74 0.5" },
    { action: "command", command: "/fill -2 69 0 2 69 5 minecraft:stone replace" },
  ],
  steps: [
    { action: "command", command: "/summon minecraft:sheep -1 70 2 {NoAI:1b,Color:0b}" },
    { action: "command", command: "/summon minecraft:sheep 0 70 2 {NoAI:1b,Color:0b}" },
    { action: "command", command: "/summon minecraft:sheep 1 70 2 {NoAI:1b,Color:0b}" },
    { action: "command", command: "/item replace entity @s weapon.mainhand with minecraft:shears" },
    {
      action: "wait_for_entity_count",
      radius: 8,
      entityType: "minecraft:sheep",
      count: 3,
      readyForShearing: true,
      timeoutMs: 3000,
    },
    { action: "wait_for_no_screen", timeoutMs: 5000, pollMs: 100 },
    { action: "look_at", x: 0.5, y: 70.5, z: 2.5 },
    { action: "wait_ms", durationMs: 300 },
    { action: "interact_nearest_entity", radius: 8, entityType: "minecraft:sheep", hand: "main_hand" },
    { action: "wait_ms", durationMs: 50 },
    { action: "interact_nearest_entity", radius: 8, entityType: "minecraft:sheep", hand: "main_hand" },
    { action: "wait_ms", durationMs: 50 },
    { action: "interact_nearest_entity", radius: 8, entityType: "minecraft:sheep", hand: "main_hand" },
    { action: "wait_ms", durationMs: 50 },
    { action: "interact_nearest_entity", radius: 8, entityType: "minecraft:sheep", hand: "main_hand" },
    { action: "wait_ms", durationMs: 300 },
    {
      action: "wait_for_entity_count",
      radius: 8,
      entityType: "minecraft:sheep",
      count: 3,
      readyForShearing: false,
      timeoutMs: 3000,
    },
    { action: "screenshot", name: "snapshears-shear-burst" },
  ],
  cleanup: [
    { action: "command", command: "/tp @s 0 70 0" },
    { action: "command", command: "/clear @s" },
    { action: "command", command: "/kill @e[type=minecraft:sheep,distance=..16]" },
    { action: "command", command: "/kill @e[type=minecraft:item,distance=..16]" },
    {
      action: "wait_for_entity_count",
      radius: 16,
      entityType: "minecraft:sheep",
      count: 0,
      timeoutMs: 5000,
    },
    {
      action: "wait_for_entity_count",
      radius: 16,
      entityType: "minecraft:item",
      count: 0,
      timeoutMs: 5000,
    },
    { action: "command", command: "/fill -2 69 0 2 69 5 minecraft:air replace" },
    { action: "command", command: "/fill -2 70 0 2 74 5 minecraft:air replace" },
  ],
} as ScenarioDefinition;

const sneakingColorBurstDefinition = {
  name: "snapshears-sneaking-color-burst",
  setup: [
    { action: "command", command: "/tp @s 0 70 0" },
    { action: "command", command: "/kill @e[type=minecraft:sheep,distance=..16]" },
    { action: "command", command: "/kill @e[type=minecraft:item,distance=..16]" },
    {
      action: "wait_for_entity_count",
      radius: 16,
      entityType: "minecraft:sheep",
      count: 0,
      timeoutMs: 5000,
    },
    {
      action: "wait_for_entity_count",
      radius: 16,
      entityType: "minecraft:item",
      count: 0,
      timeoutMs: 5000,
    },
    { action: "command", command: "/fill -2 69 0 2 69 5 minecraft:air replace" },
    { action: "command", command: "/fill -2 70 0 2 74 5 minecraft:air replace" },
    { action: "command", command: "/clear @s" },
    { action: "command", command: "/gamemode survival @s" },
    { action: "command", command: "/tp @s 0.5 74 0.5" },
    { action: "command", command: "/fill -2 69 0 2 69 5 minecraft:stone replace" },
  ],
  steps: [
    { action: "command", command: "/summon minecraft:sheep -1 70 2 {NoAI:1b,Color:0b}" },
    { action: "command", command: "/summon minecraft:sheep 0 70 2 {NoAI:1b,Color:0b}" },
    { action: "command", command: "/summon minecraft:sheep 1 70 2 {NoAI:1b,Color:14b}" },
    { action: "command", command: "/item replace entity @s weapon.mainhand with minecraft:shears" },
    {
      action: "wait_for_entity_count",
      radius: 8,
      entityType: "minecraft:sheep",
      count: 3,
      readyForShearing: true,
      timeoutMs: 3000,
    },
    { action: "wait_for_no_screen", timeoutMs: 5000, pollMs: 100 },
    { action: "look_at", x: 0.5, y: 70.5, z: 2.5 },
    { action: "key_state", key: 340, pressed: true },
    { action: "wait_ms", durationMs: 300 },
    { action: "interact_nearest_entity", radius: 8, entityType: "minecraft:sheep", hand: "main_hand" },
    { action: "wait_ms", durationMs: 50 },
    { action: "interact_nearest_entity", radius: 8, entityType: "minecraft:sheep", hand: "main_hand" },
    { action: "wait_ms", durationMs: 50 },
    { action: "interact_nearest_entity", radius: 8, entityType: "minecraft:sheep", hand: "main_hand" },
    { action: "wait_ms", durationMs: 50 },
    { action: "interact_nearest_entity", radius: 8, entityType: "minecraft:sheep", hand: "main_hand" },
    { action: "wait_ms", durationMs: 300 },
    { action: "key_state", key: 340, pressed: false },
  ],
} as ScenarioDefinition;
