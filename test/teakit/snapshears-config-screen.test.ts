import { Capability, Readiness, describe, expect, test } from "@teakit/test";
import type { ClientScreen, TeaKitTestContext } from "@teakit/test";

const CONFIG_SCREEN = "com.iamkaf.konfig.impl.v1.client.screen.KonfigConfigScreen";
const GLFW_KEY_ESCAPE = 256;

describe.configure({
  timeout: "4m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [Capability.ClientScreen, Capability.ClientScreens, Capability.ClientScreenshot, Capability.RuntimeTiming],
});

describe("SnapShears config screen", () => {
  test("opens from the loader's mod list and lists every setting", async (ctx) => {
    const loader = (await ctx.runtime.health()).loader ?? "";
    try {
      await openConfigScreen(ctx, loader);

      const labels = new Set<string>();
      for (let step = 0; step < 12; step++) {
        const screen = await ctx.client.screen();
        screen.lists().entries().forEach((entry) => labels.add(entry.label));
        if (["Snap Radius", "Only Same Color", "Bonus Wool", "Shearing Items"].every((label) => labels.has(label))) break;
        await screen.scroll({ vertical: -3 });
        await ctx.runtime.wait(150);
      }

      for (const label of ["Snap Radius", "Only Same Color", "Bonus Wool", "Shearing Items"]) {
        expect(labels.has(label)).toBe(true);
      }
      await ctx.client.screenshot(`snapshears-config-${loader}`);
    } finally {
      await leaveMenus(ctx);
    }
  });
});

async function openConfigScreen(ctx: TeaKitTestContext, loader: string): Promise<ClientScreen> {
  let screen = await showMenuWithMods(ctx);
  await screen.widgets().activate({ label: "Mods", contains: true });
  screen = await ctx.client.waitForScreen("Mods", { timeoutMs: 5_000 });

  if (loader === "fabric") {
    screen = await screen.scroll({ vertical: -10 });
    await ctx.runtime.wait(250);
  }
  screen = await selectMod(ctx, "SnapShears");

  if (loader === "fabric") {
    await activateModMenuConfigure(screen);
  } else if (loader === "forge" || loader === "neoforge") {
    await screen.widgets().activate({ label: "Config", nth: 0 });
  } else {
    throw new Error(`Unsupported loader: ${loader}`);
  }

  screen = await ctx.client.waitForScreen(CONFIG_SCREEN, { timeoutMs: 10_000 });
  await ctx.runtime.wait(500);
  return screen;
}

// Mod lists do not reliably select an entry through activate(), so scroll to it and click until it reports selected.
async function selectMod(ctx: TeaKitTestContext, label: string): Promise<ClientScreen> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 10_000) {
    const screen = await ctx.client.screen();
    const entry = screen.lists().entries().find((candidate) => candidate.label.includes(label));
    if (entry?.selected) return screen;
    if (entry != null) {
      await ctx.client.click({ x: entry.x + entry.width / 2, y: entry.y + entry.height / 2, button: 0 });
    } else {
      await screen.scroll({ vertical: -2 });
    }
    await ctx.runtime.wait(200);
  }
  const labels = (await ctx.client.screen()).lists().entries().map((entry) => entry.label);
  throw new Error(`Timed out selecting mod list entry ${label}; visible entries: ${labels.join(", ")}`);
}

// Mod Menu labels its configure button on newer versions; older versions only expose the icon widget.
async function activateModMenuConfigure(screen: ClientScreen): Promise<void> {
  if (screen.widgets().all().some((widget) => widget.label === "Configure...")) {
    await screen.widgets().activate({ label: "Configure..." });
    return;
  }
  await screen.widgets().activate({ widgetClass: "com.terraformersmc.modmenu.gui.widget.LegacyTexturedButtonWidget", nth: 1 });
}

// The mod list hangs off the title screen's and the pause menu's Mods button. Which one shows depends on whether the
// suite's world is already loaded, so press Escape until a screen with that button is up.
async function showMenuWithMods(ctx: TeaKitTestContext): Promise<ClientScreen> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 60_000) {
    const screen = await ctx.client.screen();
    const widgets = screen.widgets().all();
    if (widgets.some((widget) => widget.label.startsWith("Mods"))) {
      await ctx.runtime.wait(screen.titleScreen ? 3500 : 500);
      return ctx.client.screen();
    }
    if (widgets.some((widget) => widget.label === "Proceed to main menu")) {
      await screen.widgets().activate({ label: "Proceed to main menu" });
    } else {
      await ctx.client.key(GLFW_KEY_ESCAPE);
    }
    await ctx.runtime.wait(300);
  }
  throw new Error(`No screen with a Mods button, last on ${(await ctx.client.screen()).screenClass}`);
}

// Leave no screen open for the tests that follow. Escape backs out of the config screen, the mod list and the pause
// menu, and the next screen can take a moment to appear, so require the HUD on two reads in a row.
async function leaveMenus(ctx: TeaKitTestContext): Promise<void> {
  let clearReads = 0;
  for (let attempt = 0; attempt < 20 && clearReads < 2; attempt++) {
    if ((await ctx.client.screen()).open) {
      clearReads = 0;
      await ctx.client.key(GLFW_KEY_ESCAPE);
    } else {
      clearReads++;
    }
    await ctx.runtime.wait(250);
  }
}
