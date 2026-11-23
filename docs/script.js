const TOWERS = [
  {
    id: 1,
    title: "Sunlit Scaffolding",
    biome: "Golden Sunset Temple",
    mechanics: "Classic Slopes • Bamboo Hammer • Lotus Shield",
    description:
      "Master the basics on slanted bamboo-and-steel girders. Leap cleanly over rolling wooden barrels to stack Jump-Over Combos and collect all 3 Golden Bamboo Shoots.",
    target3Star: "3,200 pts",
    parTime: "50s",
    accent: "#FF7043",
    sky: "linear-gradient(180deg, #173857 0%, #f28f47 100%)",
    girder: "#DB4045",
    trim: "#FFCC40"
  },
  {
    id: 2,
    title: "Jade Foundry",
    biome: "Emerald Twilight Factory",
    mechanics: "Conveyors • Spring Pads • Jade Plume Double Jump",
    description:
      "Battle conveyor belts pushing against your stride, launch across tiers using elastic coil springs, and outpace fast blue barrels.",
    target3Star: "4,000 pts",
    parTime: "55s",
    accent: "#2EE59D",
    sky: "linear-gradient(180deg, #0A2429 0%, #1C7A5C 100%)",
    girder: "#24A685",
    trim: "#73FFB8"
  },
  {
    id: 3,
    title: "Crimson Pagoda",
    biome: "Neon-Red Night",
    mechanics: "Sky Elevators • Flame Spirits • Blaze Chili",
    description:
      "Time your leaps onto moving horizontal and vertical sky platforms while dodging Flame Spirits that chase you up ladders!",
    target3Star: "4,600 pts",
    parTime: "60s",
    accent: "#FF3D57",
    sky: "linear-gradient(180deg, #1C0D29 0%, #94243D 100%)",
    girder: "#D62E4D",
    trim: "#FFB838"
  },
  {
    id: 4,
    title: "Aurora Sky-Bridge",
    biome: "Aurora Cyan Observatory",
    mechanics: "Crumbling Bridges • Spike Barrels • Double Jump",
    description:
      "Sprint across unstable bamboo log bridges that shatter 0.65 seconds after touch while avoiding high-speed vertical spike barrels.",
    target3Star: "5,200 pts",
    parTime: "65s",
    accent: "#A974FF",
    sky: "linear-gradient(180deg, #0F173D 0%, #3D8FB8 100%)",
    girder: "#5270E0",
    trim: "#59F5FF"
  },
  {
    id: 5,
    title: "Iron Summit Finale",
    biome: "Volcanic Gold Citadel",
    mechanics: "3 Golden Rivets • All Hazards • Mecha-Kong",
    description:
      "Stomp all 3 Golden Rivet Switches across the fortress to stun Mecha-Kong and unlock the final Rescue Gong at the summit!",
    target3Star: "6,500 pts",
    parTime: "75s",
    accent: "#FFCA28",
    sky: "linear-gradient(180deg, #240F0D 0%, #D1571F 100%)",
    girder: "#474D5C",
    trim: "#FFC72E"
  }
];

function renderTowerExplorer() {
  const tabsEl = document.getElementById("tower-tabs");
  const titleEl = document.getElementById("tower-title");
  const biomeEl = document.getElementById("tower-biome");
  const descEl = document.getElementById("tower-desc");
  const mechEl = document.getElementById("tower-mechanics");
  const scoreEl = document.getElementById("tower-score");
  const parEl = document.getElementById("tower-par");
  const previewEl = document.getElementById("tower-preview");
  const previewLabelEl = document.getElementById("tower-preview-label");
  const girders = previewEl.querySelectorAll(".girder-preview");

  function selectTower(tower) {
    tabsEl.querySelectorAll(".tower-tab").forEach((btn) => {
      const isActive = Number(btn.dataset.id) === tower.id;
      btn.classList.toggle("active", isActive);
      btn.style.background = isActive ? tower.accent : "";
    });

    titleEl.textContent = `Tower ${tower.id}: ${tower.title}`;
    biomeEl.textContent = tower.biome;
    biomeEl.style.color = tower.accent;
    descEl.textContent = tower.description;
    mechEl.textContent = tower.mechanics;
    scoreEl.textContent = tower.target3Star;
    parEl.textContent = tower.parTime;

    previewEl.style.background = tower.sky;
    previewLabelEl.textContent = `${tower.biome} • 3D Biome Preview`;
    girders.forEach((g) => {
      g.style.background = `linear-gradient(180deg, ${tower.trim} 0%, ${tower.girder} 38%)`;
    });
  }

  TOWERS.forEach((tower) => {
    const btn = document.createElement("button");
    btn.className = "tower-tab";
    btn.dataset.id = String(tower.id);
    btn.textContent = `${tower.id}. ${tower.title}`;
    btn.addEventListener("click", () => selectTower(tower));
    tabsEl.appendChild(btn);
  });

  selectTower(TOWERS[0]);
}

document.addEventListener("DOMContentLoaded", renderTowerExplorer);
