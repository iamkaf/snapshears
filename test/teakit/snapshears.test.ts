import { Capability, Readiness, describe, expect, pos, test } from "@teakit/test";
import type { TeaKitTestContext } from "@teakit/test";

const origin = pos(0.5, 70, 0.5);

describe.configure({
  timeout: "6m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [
    Capability.ClientInput,
    Capability.ClientScreens,
    Capability.ClientScreenshot,
    Capability.PlayerInteractions,
    Capability.RuntimeTiming,
    Capability.ServerCommands,
    Capability.WorldEntities,
  ],
});

describe("SnapShears", () => {
  test("shears nearby sheep in a burst", async (ctx) => {
    await prepare(ctx, [0, 0, 0]);

    try {
      await shearNearest(ctx);
      await ctx.entities.query({
        type: "minecraft:sheep",
        origin,
        radius: 8,
        readyForShearing: false,
      }).waitForCount(3, { timeout: "3s" });
      await ctx.client.screenshot("snapshears-shear-burst");
    } finally {
      await cleanup(ctx);
    }
  });

  test("sneaking narrows a burst to the target sheep color", async (ctx) => {
    await prepare(ctx, [0, 0, 14]);

    try {
      await ctx.client.keyState(340, true);
      await shearNearest(ctx);

      const sheepData = await ctx.commands.run(
        "/execute as @e[type=minecraft:sheep,distance=..8] run data get entity @s",
        { captureOutput: true },
      );
      const sheepLines = Array.isArray(sheepData.output) ? sheepData.output : [];
      const shearedWhite = sheepLines.find((line) => line.includes("Color: 0b") && line.includes("Sheared: 1b"));
      const red = sheepLines.find((line) => line.includes("Color: 14b"));

      expect(shearedWhite).toBeTruthy();
      expect(red).toContain("Sheared: 0b");
    } finally {
      await ctx.client.keyState(340, false);
      await cleanup(ctx);
    }
  });
});

async function prepare(ctx: TeaKitTestContext, colors: readonly number[]) {
  await cleanup(ctx);
  await ctx.commands.batch([
    "/gamemode survival @s",
    "/tp @s 0.5 74 0.5",
    "/fill -2 69 0 2 69 5 minecraft:stone replace",
    "/fill -2 70 0 2 74 5 minecraft:air replace",
    "/item replace entity @s weapon.mainhand with minecraft:shears",
    ...colors.map((color, index) =>
      `/summon minecraft:sheep ${index - 1} 70 2 {NoAI:1b,Color:${color}b}`),
  ]);

  await ctx.entities.query({
    type: "minecraft:sheep",
    origin,
    radius: 8,
    readyForShearing: true,
  }).waitForCount(colors.length, { timeout: "3s" });
  await ctx.client.closeMenus();
  await ctx.player.lookAt(pos(0.5, 70.5, 2.5));
  await ctx.runtime.wait(300);
}

async function shearNearest(ctx: TeaKitTestContext) {
  const sheep = await ctx.entities.nearest("minecraft:sheep", origin);
  expect(sheep).toBeTruthy();

  if (!sheep) throw new Error("No sheep available to shear");
  await ctx.player.useItemOnEntity(sheep);
  await ctx.runtime.wait(300);
}

async function cleanup(ctx: TeaKitTestContext) {
  await ctx.commands.batch([
    "/tp @s 0 70 0",
    "/clear @s",
    "/kill @e[type=minecraft:sheep,distance=..16]",
    "/kill @e[type=minecraft:item,distance=..16]",
    "/fill -2 69 0 2 69 5 minecraft:air replace",
    "/fill -2 70 0 2 74 5 minecraft:air replace",
  ]);

  await ctx.entities.query({ type: "minecraft:sheep", origin, radius: 16 })
    .waitForCount(0, { timeout: "5s" });
}
