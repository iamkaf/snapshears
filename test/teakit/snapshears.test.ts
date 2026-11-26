import { Capability, Readiness, describe, expect, pos, test } from "@teakit/test";
import type { EntityRef, ItemId, TeaKitTestContext } from "@teakit/test";

const origin = pos(0.5, 70, 0.5);
const TARGET_X = 0.5;
const TARGET_Z = 2.5;
const WHITE = 0;
const BLUE = 11;
const RED = 14;
const DEFAULT_SHEARS = "minecraft:shears,#c:tools/shear";

type SheepSpec = { x: number; color?: number; sheared?: boolean; baby?: boolean };

describe.configure({
  timeout: "6m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [
    Capability.ClientInput,
    Capability.ClientScreens,
    Capability.ClientScreenshot,
    Capability.PlayerInteractions,
    Capability.PlayerInventory,
    Capability.RuntimeTiming,
    Capability.ServerCommands,
    Capability.WorldEntities,
    Capability.WorldLoot,
  ],
});

describe("SnapShears", () => {
  test("shears nearby sheep in a burst", async (ctx) => {
    await prepare(ctx, [{ x: TARGET_X }, { x: TARGET_X + 1.5 }, { x: TARGET_X - 1.5 }]);

    try {
      await snap(ctx);
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
    await prepare(ctx, [
      { x: TARGET_X, color: WHITE },
      { x: TARGET_X + 1.5, color: WHITE },
      { x: TARGET_X - 1.5, color: RED },
    ]);

    try {
      await ctx.client.keyState(340, true);
      await snap(ctx);

      await expectSheared(ctx, TARGET_X, true);
      await expectSheared(ctx, TARGET_X + 1.5, true);
      await expectSheared(ctx, TARGET_X - 1.5, false);
    } finally {
      await ctx.client.keyState(340, false);
      await cleanup(ctx);
    }
  });

  test("the only_same_color setting narrows a burst without sneaking", async (ctx) => {
    await prepare(ctx, [
      { x: TARGET_X, color: WHITE },
      { x: TARGET_X + 1.5, color: WHITE },
      { x: TARGET_X - 1.5, color: RED },
    ]);

    try {
      await debug(ctx, "only_same_color true");
      await snap(ctx);

      await expectSheared(ctx, TARGET_X + 1.5, true);
      await expectSheared(ctx, TARGET_X - 1.5, false);
    } finally {
      await cleanup(ctx);
    }
  });

  test("the radius setting decides how far a burst reaches", async (ctx) => {
    try {
      // The default radius is 3 blocks around the clicked sheep.
      await prepare(ctx, [{ x: TARGET_X }, { x: TARGET_X + 2.5 }, { x: TARGET_X + 4 }]);
      await snap(ctx);
      await expectSheared(ctx, TARGET_X + 2.5, true);
      await expectSheared(ctx, TARGET_X + 4, false);

      // Radius 0 only reaches a sheep standing in the same spot.
      await debug(ctx, "radius 0");
      await prepare(ctx, [{ x: TARGET_X }, { x: TARGET_X }, { x: TARGET_X + 1 }], false);
      await snap(ctx);
      expect(await shearedCount(ctx)).toBe(2);
      await expectSheared(ctx, TARGET_X + 1, false);

      await debug(ctx, "radius 16");
      await prepare(ctx, [{ x: TARGET_X }, { x: TARGET_X + 12 }], false);
      await snap(ctx);
      await expectSheared(ctx, TARGET_X + 12, true);
    } finally {
      await cleanup(ctx);
    }
  });

  test("bonus wool drops extra wool of each sheared sheep's color", async (ctx) => {
    try {
      // Shearing drops 1 to 3 wool, so a bonus of 3 always puts at least 4 of a sheep's color on the ground.
      await prepare(ctx, [{ x: TARGET_X, color: WHITE }, { x: TARGET_X + 2.5, color: RED }, { x: TARGET_X - 2, color: BLUE }]);
      await snap(ctx);
      const plain = await waitForWool(ctx, 1);
      expect(plain.red).toBeGreaterThanOrEqual(1);
      expect(plain.red).toBeLessThanOrEqual(3);
      expect(plain.blue).toBeGreaterThanOrEqual(1);
      expect(plain.blue).toBeLessThanOrEqual(3);

      await debug(ctx, "bonus_wool 3");
      await prepare(ctx, [{ x: TARGET_X, color: WHITE }, { x: TARGET_X + 2.5, color: RED }, { x: TARGET_X - 2, color: BLUE }], false);
      await snap(ctx);
      const bonus = await waitForWool(ctx, 4);
      expect(bonus.red).toBeGreaterThanOrEqual(4);
      expect(bonus.blue).toBeGreaterThanOrEqual(4);
      // The clicked sheep drops what vanilla shearing drops. The player may stand close enough to pick some of it up.
      expect(bonus.white).toBeLessThanOrEqual(3);
    } finally {
      await cleanup(ctx);
    }
  });

  test("the shears list decides which items snap", async (ctx) => {
    try {
      // The default list resolves its tag entry and a plain stick snaps nothing.
      await prepare(ctx, [{ x: TARGET_X }, { x: TARGET_X + 1.5 }]);
      const tag = await ctx.commands.run("/clear @s #c:tools/shear 0");
      expect(tag.success).toBe(true);
      await holdItem(ctx, "minecraft:stick");
      await snap(ctx);
      expect(await shearedCount(ctx)).toBe(0);

      // An item entry replaces the defaults: shears no longer snap, a sword does.
      await debug(ctx, "shears minecraft:iron_sword");
      await prepare(ctx, [{ x: TARGET_X }, { x: TARGET_X + 1.5 }], false);
      await snap(ctx);
      await expectSheared(ctx, TARGET_X + 1.5, false);
      await holdItem(ctx, "minecraft:iron_sword");
      await snap(ctx);
      await expectSheared(ctx, TARGET_X + 1.5, true);

      // A tag entry matches every item in the tag.
      await debug(ctx, "shears #minecraft:swords");
      await prepare(ctx, [{ x: TARGET_X }, { x: TARGET_X + 1.5 }], false);
      await holdItem(ctx, "minecraft:iron_sword");
      await snap(ctx);
      await expectSheared(ctx, TARGET_X + 1.5, true);
    } finally {
      await cleanup(ctx);
    }
  });

  test("a snap wears the shears once per sheared sheep and skips sheared and baby sheep", async (ctx) => {
    try {
      // Newer versions wear shears for a click on an already sheared sheep too, so compare against a plain click.
      await prepare(ctx, [{ x: TARGET_X, sheared: true }]);
      await snap(ctx);
      const plainClick = await damageOf(ctx, "weapon.mainhand");

      await prepare(ctx, [
        { x: TARGET_X, sheared: true },
        { x: TARGET_X + 1.5 },
        { x: TARGET_X - 1.5, sheared: true },
        { x: TARGET_X + 2.5, baby: true },
      ], false);
      await snap(ctx);
      await expectSheared(ctx, TARGET_X + 1.5, true);
      await expectSheared(ctx, TARGET_X + 2.5, false);
      // One point for the one sheep the burst added.
      await expectWear(ctx, await damageOf(ctx, "weapon.mainhand"), plainClick, 1);
    } finally {
      await cleanup(ctx);
    }
  });

  test("only a falling hit snaps", async (ctx) => {
    await prepare(ctx, [{ x: TARGET_X }, { x: TARGET_X + 1.5 }]);

    try {
      await snap(ctx, { fall: false });
      await expectSheared(ctx, TARGET_X, true);
      await expectSheared(ctx, TARGET_X + 1.5, false);
      expect(await damageOf(ctx, "weapon.mainhand")).toBe(1);
    } finally {
      await cleanup(ctx);
    }
  });

  test("the main hand shears first and the off hand shears alone", async (ctx) => {
    const sheep = [{ x: TARGET_X, sheared: true }, { x: TARGET_X + 1.5 }, { x: TARGET_X - 1.5 }];
    try {
      await prepare(ctx, [{ x: TARGET_X, sheared: true }]);
      await snap(ctx);
      const plainClick = await damageOf(ctx, "weapon.mainhand");

      await prepare(ctx, sheep, false);
      await ctx.commands.run("/item replace entity @s weapon.offhand with minecraft:shears");
      await snap(ctx);
      expect(await shearedCount(ctx)).toBe(3);
      await expectWear(ctx, await damageOf(ctx, "weapon.mainhand"), plainClick, 2);
      expect(await damageOf(ctx, "weapon.offhand")).toBe(0);

      // With nothing to shear in the main hand, the off hand shears carry the burst.
      await prepare(ctx, [{ x: TARGET_X, sheared: true }], false);
      await ctx.commands.run("/item replace entity @s weapon.mainhand with minecraft:stick");
      await ctx.commands.run("/item replace entity @s weapon.offhand with minecraft:shears");
      await snap(ctx, { hand: "off_hand" });
      const offHandClick = await damageOf(ctx, "weapon.offhand");

      await prepare(ctx, sheep, false);
      await ctx.commands.run("/item replace entity @s weapon.mainhand with minecraft:stick");
      await ctx.commands.run("/item replace entity @s weapon.offhand with minecraft:shears");
      await snap(ctx, { hand: "off_hand" });
      expect(await shearedCount(ctx)).toBe(3);
      await expectWear(ctx, await damageOf(ctx, "weapon.offhand"), offHandClick, 2);
    } finally {
      await cleanup(ctx);
    }
  });

  test("reloading restores the saved config", async (ctx) => {
    try {
      await debug(ctx, "radius 5");
      await debug(ctx, "unsaved radius 1");
      await debug(ctx, "unsaved bonus_wool 2");

      const reload = await ctx.commands.run("/snapshears reload", { captureOutput: true });
      expect(reload.success).toBe(true);
      const values = await ctx.commands.run("/snapshears debug get", { captureOutput: true });
      // The file's values come back over what was changed in memory.
      expect(values.output.join("\n")).toContain("local radius=5 bonus_wool=0");
    } finally {
      await cleanup(ctx);
    }
  });
});

// A saved change reaches the value the gameplay code reads through a config sync, so wait for it to show.
async function debug(ctx: TeaKitTestContext, args: string) {
  await ctx.commands.assert(`/snapshears debug ${args}`);
  const [key, ...rest] = args.split(" ");
  if (key === "unsaved") return;
  await waitForConfig(ctx, `${key}=${rest.join(" ")}`);
}

async function waitForConfig(ctx: TeaKitTestContext, expected: string) {
  await expect(async () => {
    const result = await ctx.commands.run("/snapshears debug get", { captureOutput: true });
    return result.output.join("").includes(expected);
  }).toEventuallyEqual(true, { timeout: "5s" });
}

async function prepare(ctx: TeaKitTestContext, sheep: readonly SheepSpec[], resetConfig = true) {
  await cleanup(ctx, resetConfig);
  await ctx.commands.batch([
    "/gamemode survival @s",
    "/tp @s 0.5 70 0.5",
    "/fill -4 69 -2 18 69 8 minecraft:stone replace",
    "/fill -4 70 -2 18 74 8 minecraft:air replace",
    "/item replace entity @s weapon.mainhand with minecraft:shears",
    ...sheep.map((spec) => {
      const nbt = [`NoAI:1b`, `Color:${spec.color ?? WHITE}b`];
      if (spec.sheared) nbt.push("Sheared:1b");
      if (spec.baby) nbt.push("Age:-24000");
      // Whole-number command coordinates snap to the block center, so spell out the decimals.
      return `/summon minecraft:sheep ${spec.x.toFixed(2)} 70 ${TARGET_Z.toFixed(2)} {${nbt.join(",")}}`;
    }),
  ]);

  await ctx.entities.query({ type: "minecraft:sheep", origin, radius: 30 })
    .waitForCount(sheep.length, { timeout: "3s" });
  await ctx.client.closeMenus();
  await ctx.player.lookAt(pos(TARGET_X, 70.5, TARGET_Z));
  await ctx.player.inventory().waitForItem("minecraft:shears", { selected: true, timeout: "5s" });
  await ctx.runtime.wait(500);
}

/** Hits the sheep at the target position, falling onto it unless told to stand. */
async function snap(ctx: TeaKitTestContext, options: { fall?: boolean; hand?: "main_hand" | "off_hand" } = {}) {
  const sheep = await sheepAt(ctx, TARGET_X);
  expect(sheep).toBeTruthy();
  if (!sheep) throw new Error("No sheep at the target position");

  // Begin falling only after the client has received the fixture and held item.
  if (options.fall !== false) {
    await ctx.player.teleport(pos(0.5, 72, 0.5));
    await ctx.runtime.wait(150);
  }
  await ctx.player.useItemOnEntity(sheep, undefined, options.hand ? { hand: options.hand } : undefined);
  await ctx.runtime.wait(300);
}

async function holdItem(ctx: TeaKitTestContext, item: ItemId) {
  await ctx.commands.assert(`/item replace entity @s weapon.mainhand with ${item}`);
  await ctx.player.inventory().waitForItem(item, { selected: true, timeout: "5s" });
}

async function sheepAt(ctx: TeaKitTestContext, x: number): Promise<EntityRef | null> {
  return ctx.entities.query({ type: "minecraft:sheep", origin: pos(x, 70, TARGET_Z), radius: 0.4 }).nearest();
}

// Sheep data is the one reliable source for the sheared flag: baby sheep also read as not ready for shearing.
async function isSheared(ctx: TeaKitTestContext, x: number): Promise<boolean> {
  const result = await ctx.commands.run(
    `/execute if entity @e[type=minecraft:sheep,x=${x.toFixed(2)},y=70,z=${TARGET_Z.toFixed(2)},distance=..0.4,nbt={Sheared:1b}]`,
  );
  return result.success;
}

async function expectSheared(ctx: TeaKitTestContext, x: number, expected: boolean) {
  if ((await isSheared(ctx, x)) === expected) return;
  const sheep = await ctx.commands.run(
    "/execute as @e[type=minecraft:sheep,distance=..40] run data get entity @s Pos",
    { captureOutput: true },
  );
  const flags = await ctx.commands.run(
    "/execute as @e[type=minecraft:sheep,distance=..40] run data get entity @s Sheared",
    { captureOutput: true },
  );
  const config = await ctx.commands.run("/snapshears debug get", { captureOutput: true });
  throw new Error(
    `Sheep at x=${x} should${expected ? "" : " not"} be sheared.\n${sheep.output.join("\n")}\n${flags.output.join("\n")}\n${config.output.join("")}`,
  );
}

async function shearedCount(ctx: TeaKitTestContext): Promise<number> {
  const result = await ctx.commands.run("/execute if entity @e[type=minecraft:sheep,distance=..40,nbt={Sheared:1b}]");
  return result.success ? result.result : 0;
}

async function woolCounts(ctx: TeaKitTestContext) {
  const count = async (color: string) => {
    const wool = await ctx.loot.near(pos(TARGET_X, 70, TARGET_Z), { item: `minecraft:${color}_wool`, radius: 8 });
    return wool.reduce((total, entity) => total + (entity.count ?? 1), 0);
  };
  return { white: await count("white"), red: await count("red"), blue: await count("blue") };
}

// Reads the wool on the ground as soon as each sheared sheep's color has dropped enough, before the player can walk
// into any of it.
async function waitForWool(ctx: TeaKitTestContext, atLeast: number) {
  let counts = await woolCounts(ctx);
  await expect(async () => {
    counts = await woolCounts(ctx);
    return counts.red >= atLeast && counts.blue >= atLeast;
  }).toEventuallyEqual(true, { timeout: "5s" });
  return counts;
}

// Shears wear one point per sheep the burst adds on top of a plain click. From 1.21.11 the click's own wear on an
// already sheared sheep varies from run to run, so those versions only have to stay within one point of that.
async function expectWear(ctx: TeaKitTestContext, actual: number, plainClick: number, added: number) {
  const version = (await ctx.runtime.summary()).minecraftVersion ?? "0";
  const [major, minor, patch] = version.split(".").map((part) => Number.parseInt(part, 10) || 0);
  const noisy = major > 1 || minor > 21 || (minor === 21 && patch >= 11);
  if (!noisy) {
    expect(actual).toBe(plainClick + added);
  } else {
    expect(actual).toBeGreaterThanOrEqual(plainClick + added - 1);
    expect(actual).toBeLessThanOrEqual(plainClick + added);
  }
}

// The damage on the shears in a slot, found through the item predicate so it reads the same on every version.
async function damageOf(ctx: TeaKitTestContext, slot: string): Promise<number> {
  for (let damage = 0; damage <= 12; damage++) {
    const result = await ctx.commands.run(`/execute if items entity @s ${slot} minecraft:shears[minecraft:damage=${damage}]`);
    if (result.success) return damage;
  }
  throw new Error(`No shears with a damage under 13 in ${slot}`);
}

async function cleanup(ctx: TeaKitTestContext, resetConfig = true) {
  await ctx.commands.batch([
    "/tp @s 0 70 0",
    "/clear @s",
    "/kill @e[type=minecraft:sheep,distance=..40]",
    "/kill @e[type=minecraft:item,distance=..40]",
    // Leave the floor: taking it out from under the player drops them out of the arena on tall worlds.
    "/fill -4 70 -2 18 74 8 minecraft:air replace",
    ...(resetConfig
      ? [
          "/snapshears debug radius 3",
          "/snapshears debug bonus_wool 0",
          "/snapshears debug only_same_color false",
          `/snapshears debug shears ${DEFAULT_SHEARS}`,
        ]
      : []),
  ]);
  if (resetConfig) {
    await waitForConfig(ctx, `radius=3 bonus_wool=0 only_same_color=false shears=${DEFAULT_SHEARS}`);
  }

  await ctx.entities.query({ type: "minecraft:sheep", origin, radius: 30 })
    .waitForCount(0, { timeout: "5s" });
}
