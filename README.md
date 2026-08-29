<p align="center">
  <img src="logo.png" alt="StarShack Logo" width="600" height="337">
</p>

<h1 align="center">StarShack</h1>

<p align="center">
  <b>A modern, privacy-first Minecraft utility mod for 1.8.9 (Forge)</b><br>
  <i>Configuration-persistent · Extensible · Built on Novoline-bS / Raven-bS foundations</i>
</p>

<p align="center">
  <a href="https://github.com/vliantwo/starshack/releases"><img src="https://img.shields.io/github/v/release/vliantwo/starshack?style=flat-square&color=blue" alt="Releases"></a>
  <a href="https://github.com/vliantwo/starshack/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square" alt="License: GPLv3"></a>
  <a href="https://github.com/vliantwo/starshack/issues"><img src="https://img.shields.io/github/issues/vliantwo/starshack?style=flat-square" alt="Issues"></a>
  <a href="https://discord.gg/your-invite-code"><img src="https://img.shields.io/badge/Discord-Join-5865F2?style=flat-square&logo=discord" alt="Discord"></a>
</p>

---

## ✨ Features

- **🛡️ Privacy-First** — No telemetry, no analytics, no account data sent anywhere. What happens on your machine stays
  on your machine.

- **💾 Configuration-Persistent** — All settings auto-save on change (`saved` dirty-flag → tick-based flush)
  and re-load on startup. Change CPS, toggle a module, restart — your config is exactly as you left it. **No more "
  settings reset after every launch."**

- **🔌 Extensible** — Full scripting API + a clean module/setting architecture (`Setting` base class, `ModeSetting`,
  `SliderSetting`, `ButtonSetting`, …)
  makes adding new modules straightforward.

- **⚔️ Combat** — KillAura, AutoClicker (Vape V4-style: Normal / Extra / Extra+ randomization, Trigger modes, Jitter,
  Break-blocks coordination), Reach, Velocity, Criticals, AimAssist, BackTrack, and more.

- **🎨 Render** — Clean, customizable HUD with module list, TargetHUD, ESP (player / mob / item / chest / bed), Tracers,
  TNT timer, Trajectories, Chams, Nametags, Break progress, and a polished ClickGUI.

- **🚀 Movement** — Speed, Fly, BHop, LongJump, Sprint, WTap, NoSlow, Timer, Velocity, Strafe, Teleport, and movement-fix
  helpers.

- **🧱 World / Player** — Scaffold, FastMine, FastPlace, AutoTool, AutoSwap, NoFall, SafeWalk, GhostHand, AntiFireball,
  Freecam, and many more.

- **🎮 Minigames** — Dedicated modules for BedWars, SkyWars, MurderMystery, Duels, Sumo, SpeedBuilders, WoolWars,
  BridgeInfo, AutoRequeue, and stats tracking.

- **💬 Other** — AntiAFK, ChatBypass, FakeChat, NameHider, Relationships (player relations manager), HideWindow,
  Disabler, IRC, and an Anticheat utility.

> **NOTE:** StarShack is a *utility mod* intended for **private servers,
> single-player, and learning/modding research**. Use on public servers
> (especially competitive ones) is **at your own risk** and may violate
> their terms of service. The author (s) are not responsible for any bans
> or consequences.
---

## 🚀 Getting Started

### Prerequisites

| Requirement   | Version                                  |
|---------------|------------------------------------------|
| **Java JDK**  | 8 (1.8) — *other versions will NOT work* |
| **Minecraft** | 1.8.9 (Forge)                            |
| **OS**        | Windows / Linux / macOS                  |
| **RAM**       | ≥ 4 GB recommended                       |

### Clone & Build

## 📖 Usage

> A quick guide to actually *using* StarShack once it's installed and running.

### Opening the ClickGUI

- Press **Right Shift (`RSHIFT`)** by default to open the ClickGUI.
- To change the GUI keybind, go to the `Settings` module (in the `Client` category) and rebind it.

### Navigating the ClickGUI

| Action                                            | What it does                                                           |
|---------------------------------------------------|------------------------------------------------------------------------|
| **Left Click** on a module                        | Toggle the module on / off                                             |
| **Right Click** on a module                       | Open / close its settings panel on the right                           |
| **Middle Click** (scroll-wheel press) on a module | Enter keybind mode — press any key to bind, or `ESC` to clear the bind |
| **Scroll**                                        | Scroll through the module list or settings panel                       |

### Rebinding a Module (e.g. bind KillAura to `R`)

1. Open the ClickGUI and find the module (e.g. **KillAura** in the `Combat` category).
2. **Middle Click** the module row — its name changes to `Press a key...`.
3. Press the key you want (e.g. **R**) → the module is now bound to `R`.
4. In-game, pressing **R** toggles KillAura on / off.

To **cancel** a bind: Middle Click the module → press **ESC** (shows `NONE`).  
To **change** a bind: just repeat the steps above with a different key.

> **Note:** Only modules with `canBeEnabled = true` (most combat / movement modules) can be bound. Configuration-style
> modules (like `Settings`) cannot.

### Managing Settings

- **Slider** — Click and drag to adjust numeric values (e.g. CPS, delay, reach).
- **Dropdown / Mode** — Click the current mode to expand the list, then click an option to switch (e.g. change ESP from
  `Box` to `Outline`, `Glow`, etc.).
- **Toggle Button (Switch)** — Click to enable / disable a sub-feature.
- **Keybind** — Click the key name, then press a new key to rebind.
- **Color Picker** — Click the color bar to adjust Hue / Saturation / Brightness; right-click the bar to cycle channels.
- **Text Input** — Click the text field, type, then press `Enter` to submit.

### Searching Modules

Click the **Search...** bar at the top of the module list and type — modules are filtered by name in real-time.

### Profiles & Auto-Save

- StarShack auto-saves every change to `.minecraft/starshack/profiles/default.json` (no manual save needed).
- Use the in-game command `/p load <name>` or `/p save <name>` to manage multiple profiles.
- Restart the game at any time — your settings are restored exactly as you left them.

> **Tip:** You can safely switch servers / worlds — module states and settings persist across the session.

---

## 📜 Legal & License

StarShack is free software, licensed under the **GNU General Public License v3.0 (GPLv3)**.

> **What this means:**
> - You are free to use, copy, modify, and redistribute StarShack.
> - Any distribution (including precompiled jars) **must** also be
>   released under GPLv3 with full source available.
> - You must preserve the original copyright and license notices.
> - The authors are NOT liable for any damages arising from use.

See the full license text in [LICENSE](LICENSE).

**SPDX identifier:** `GPL-3.0-only`

---

## 🙏 Credits & Acknowledgements

StarShack is built upon the foundations laid by the **Raven-bS / Novoline-bS**
family of Minecraft utility mods. Huge thanks to the original developers:

- **[Novoline-bS](https://github.com/Ij1chi-Nijika/Novoline-bS)**
  by [Ij1chi-Nijika](https://github.com/Ij1chi-Nijika) — the base this project was forked from. Much of the module and
  setting architecture, ClickGUI, and Mixin work originates here.

- **Raven-bS / Raven-XD** — the original "Raven" lineage that Novoline-bS itself extended. Core concepts (esp, aura,
  scaffolds, etc.) trace back to this community.

- **[LiquidBounce](https://github.com/CCBlueX/LiquidBounce)** — an inspiration for clean module architecture and
  scripting design.

- **Minecraft Forge / ForgeGradle** — the modding framework this project builds on top of.

- **SpongePowered Mixin** — the bytecode transformation library used for runtime patches.

This project **inherits the GPLv3 license** from its predecessors and **preserves all applicable copyright notices**. If
you believe any attribution is missing, please open an issue and it will be corrected promptly.

---

**StarShack** — *Privacy-First. Configuration-Persistent. Yours to extend.*
GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007

Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
Everyone is permitted to copy and distribute verbatim copies of this license document, but changing it is not allowed.

                            Preamble

The GNU General Public License is a free, copyleft license for software and other kinds of works.

The licenses for most software and other practical works are designed to take away your freedom to share and change the
works. By contrast, the GNU General Public License is intended to guarantee your freedom to share and change all
versions of a program--to make sure it remains free software for all its users. We, the Free Software Foundation, use
the GNU General Public License for most of our software; it applies also to any other work released this way by its
authors. You can apply it to your programs, too.

When we speak of free software, we are referring to freedom, not price. Our General Public Licenses are designed to make
sure that you have the freedom to distribute copies of free software (and charge for them if you wish), that you receive
source code or can get it if you want it, that you can change the software or use pieces of it in new free programs, and
that you know you can do these things.

To protect your rights, we need to prevent others from denying you these rights or asking you to surrender the rights.
Therefore, you have certain responsibilities if you distribute copies of the software, or if you modify it:
responsibilities to respect the freedom of others.

For example, if you distribute copies of such a program, whether gratis or for a fee, you must pass on to the recipients
the same freedoms that you received. You must make sure that they, too, receive or can get the source code. And you must
show them these terms so they know their rights.

Developers that use the GNU GPL protect your rights with two steps:
(1) assert copyright on the software, and (2) offer you this License giving you legal permission to copy, distribute
and/or modify it.

For the developers' and authors' protection, the GPL clearly explains that there is no warranty for this free software.
For both users' and authors' sake, the GPL requires that modified versions be marked as changed, so that their problems
will not be attributed erroneously to authors of previous versions.

Some devices are designed to deny users access to install or run modified versions of the software inside them, although
the manufacturer can do so. This is fundamentally incompatible with the purpose of the GPL, which is to protect users'
freedom to change the software. The systematic pattern of such abuse occurs in the practice of tivoization:
"installing" an executable on a device, but not allowing the user to run a modified version of that executable on that
device. The GPL therefore permits such devices only if they also allow running other software that has been released
under a GPL-compatible free software license, such as the user's own modifications.

The precise terms and conditions for copying, distribution and modification follow.

                       TERMS AND CONDITIONS

0. Definitions.

"This License" refers to version 3 of the GNU General Public License.

"Copyright" also means copyright-like laws that apply to other kinds of works, such as semiconductor masks.

"The Program" refers to any copyrightable work licensed under this License. Each licensee is addressed as "you".
"Licensees" and
"recipients" may be individuals or organizations.

To "modify" a work means to copy from or adapt all or part of the work in a fashion requiring copyright permission,
other than the making of an exact copy. The resulting work is called a "modified version" of the earlier work or a work
"based on" the earlier work.

A "covered work" means either the unmodified Program or a work based on the Program.

To "propagate" a work means to do anything with it that, without permission, would make you directly or secondarily
liable for infringement under applicable copyright law, except executing it on a computer or modifying a private copy.
Propagation includes copying, distribution (with or without modification), making available to the public, and in some
countries other activities as well.

To "convey" a work means any kind of propagation that enables other parties to make or receive copies. Mere interaction
with a user through a computer network, with no transfer of a copy, is not conveying.

An interactive user interface displays "Appropriate Legal Notices"
to the extent that it includes a convenient and prominently visible feature that (1) displays an appropriate copyright
notice, and (2)
tells the user that there is no warranty for the work (except to the extent that warranties are provided), that
licensees may convey the work under this License, and how to view a copy of this License. If the interface presents a
list of user commands or options, such as a menu, a prominent item in the list meets this criterion. The "source code"
for a work means the preferred form of the work for making modifications to it.  "Object code" means any non-source form
of a work.

A "Standard Interface" means an interface that either is an official standard defined by a recognized standards body,
or, in the case of interfaces specified for a particular programming language, one that is widely used among developers
working in that language.

A "Library" means a work, other than an application or a plugin for an application, that is intended to be compiled or
linked with other parts of a Larger Work on an ad hoc basis. A "Larger Work" means a work produced by combining the
Library with other independent works, none of which is derived from or based on the Library.

The "Minimal Corresponding Source" for a non-source form of a work means the source code that, when processed as
described in the specification of the work, yields the same executable file (or the same object code and, if applicable,
other artifacts) as the non-source form.

The "Corresponding Source" for a work in object code form means all the source code needed to generate, install, and
(for an executable work) run the object code and to modify the work, including scripts to control those activities.
However, it does not include the work's System Libraries, or general-purpose tools or generally available free programs
which are used unmodified in performing those activities but which are not part of the work. For example, Corresponding
Source includes interface definition files associated with source files for the work, and the source code for shared
libraries and dynamically linked subprograms that the work is specifically designed to require, such as by intimate data
communication or control flow between those subprograms and other parts of the work.

1. Source Code.

The "source code" for a work means the preferred form of the work for making modifications to it. For the purposes of
this License, object code and executable forms are not source code.

A "covered work" may be conveyed under the conditions stated in sections 3 through 6 below, in addition to those in
section 2.

2. Basic Permissions.

All rights granted under this License are granted for the term of copyright on the Program, and are irrevocable provided
the stated conditions are met. This License explicitly affirms your unlimited permission to run the unmodified Program.
The output from running a covered work is covered by this License only if the output, given its content, constitutes a
covered work. This License acknowledges your rights of fair use or other equivalent, as provided by copyright law.

You may make, run and propagate covered works that you do not convey, without conditions so long as your license
otherwise remains in force. You may convey covered works to others for the sole purpose of having them make
modifications exclusively for you, or provide you with facilities for running those works, provided that you comply with
the terms of this License in conveying all material for which you do not control copyright. Those thus making or running
the covered works for you must do so exclusively on your behalf, under your direction and control, on terms that
prohibit them from making any copies of your copyrighted material outside their relationship with you.

Conveying under any other circumstances is permitted solely under the conditions stated below. Sublicensing is not
allowed; section 10 makes it unnecessary.

3. Protecting Users' Legal Rights From Anti-Circumvention Law.

No covered work shall be deemed part of an effective technological protection measure under any applicable law
fulfilling obligations under article 11 of the WIPO copyright treaty adopted on 20 December 1996, or similar laws
prohibiting or restricting circumvention of such measures.

When you convey a covered work, you waive any legal power to forbid circumvention of technological measures affecting
exercise of the rights under this License. Users of covered works released under this License waive such measures as a
condition of accessing and using the covered work.

4. Conveying Verbatim Copies.

You may convey verbatim copies of the Program's source code as you receive it, in any medium, provided that you
conspicuously and appropriately publish on each copy an appropriate copyright notice; keep intact all notices stating
that this License and any non-permissive terms added in accord with section 7 apply to the code; keep intact all notices
of the absence of any warranty; and give all recipients a copy of this License along with the Program.

You may charge any price or no price for each copy that you convey, and you may offer support or warranty protection for
a fee.

5. Conveying Modified Source Versions.

You may convey a work based on the Program, or the modifications to produce it from the Program, in the form of source
code under the terms of section 4, provided that you also meet all of these conditions:

    a) The work must carry prominent notices stating that you modified
    it, and giving a relevant date.

    b) The work must carry prominent notices stating that it is
    released under this License and any conditions added under section
    7.  This requirement modifies the requirement in section 4 to
    "keep intact all notices".

    c) You must license the entire work, as a whole, under this
    License to anyone who comes into possession of a copy.  This
    License will therefore apply, along with any applicable section 7
    additional terms, to the whole of the work, and all its parts,
    regardless of how they are packaged.  This License gives no
    permission to license the work in any other way, but it does not
    invalidate such permission if you have separately received it.

    d) If the work has interactive user interfaces, each must display
    the Appropriate Legal Notices; however, if the Program has
    interactive interfaces that do not display Appropriate Legal
    Notices, neither does yours.

6. Conveying Non-Source Forms.

You may convey a covered work in object code or other non-source form under the terms of sections 2 and 4, and also
under the conditions stated in this section 6.

a) The Corresponding Source must be conveyed with, or made publicly available at no charge no later than 30 days after,
the conveyance of the object code. The Corresponding Source may be conveyed in a form that is commonly used for software
interchange, and need not be in a form that is convenient for making modifications.

b) If the conveying is by way of a link to a network location that the operators of that network location have the right
to control, then the Corresponding Source may be made available in the same way through the same network location,
provided that the operators do not intend to replace the Corresponding Source with a different version that is not
covered under this License.

c) The Corresponding Source need not include anything that users can regenerate automatically from other parts of the
Corresponding Source.

d) A work is not "conveyed" merely by making it accessible over a network for downloading.

e) The Corresponding Source for a work in object code form may be conveyed in any of the following ways:

    i) Convey the object code with the Corresponding Source.

    ii) Convey the object code with an offer, valid for at least
    three years and valid for as long as you offer spare parts or
    customer support for that product model, to give any third party,
    for a charge no more than your cost of physically performing
    source distribution, a copy of the Corresponding Source.  The
    offer must be attached to the object code in a way that is
    appropriate for conveying it.

    iii) Convey the object code with an offer, valid for at least
    three years, to give anyone who possesses the object code either
    (1) a copy of the Corresponding Source for all versions of the
    Program that you have conveyed in that manner, or (2) a written
    offer to provide such a copy.  The offer must be attached to the
    object code in a way that is appropriate for conveying it.

    iv) Convey the object code with a written offer to provide the
    Corresponding Source, if and only if you also received the object
    code in that manner.  You may not convey the Corresponding Source
    under this option if you received it in any other way.

    v) Convey the object code in, or embodied in, a physical product
    (including a physical distribution medium), accompanied by the
    Corresponding Source in, or embodied in, the same product.

    vi) Convey the object code in, or embodied in, a physical product
    (including a physical distribution medium), accompanied by a
    written offer, valid for at least three years and valid for as
    long as you offer spare parts or customer support for that product
    model, to give anyone who possesses the object code a copy of the
    Corresponding Source.

f) You may charge any price or no price for each copy that you convey, and you may offer support or warranty protection
for a fee.

g) You may not impose any further restrictions on the recipients' exercise of the rights granted herein. You are not
responsible for enforcing compliance by third parties with this License.

h) If you convey an object code work that is covered by this License, you must comply with the conditions of this
section 6 for the entire work, including all parts, regardless of how they are packaged. This requirement does not apply
to mere aggregation of another work not based on the Program with the work covered by this License.

i) If the conveying is by way of a link to a network location that the operators of that network location have the right
to control, then the Corresponding Source may be made available in the same way through the same network location,
provided that the operators do not intend to replace the Corresponding Source with a different version that is not
covered under this License.

j) A "User Product" is either (1) a "consumer product", which means any tangible personal property which is normally
used for personal, family, or household purposes, or (2) anything designed or sold for incorporation into a dwelling. In
determining whether a product is a consumer product, doubtful cases shall be resolved in favor of coverage. For a
particular product received by a particular user, "normally used" refers to a typical or common use of that class of
product, regardless of the status of the particular user or of the way in which the particular user actually uses, or
expects or is expected to use, the product. A product is a consumer product regardless of whether the product has
substantial commercial, industrial or non-consumer uses, unless such uses represent the only significant mode of use of
the product.

k) "Installation Information" for a User Product means any methods, procedures, authorization keys, or other information
required to install and execute modified versions of a covered work in that User Product from a modified version of its
Corresponding Source. The information must suffice to ensure that the continued functioning of the modified object code
is in no case prevented or interfered with solely as a result of modifications made by and for the user.

l) If you convey an object code work under this section 6, and the work is specifically designed to work with a User
Product, and the conveying occurs as part of a transaction in which the right of possession and use of the User Product
is transferred to the recipient in perpetuity or for a fixed term (regardless of how the transaction is characterized),
then the Corresponding Source for the work must be accompanied by the Installation Information. But this requirement
does not apply if neither you nor any third party retains the ability to install modified object code on the User
Product (for example, the work has been installed in ROM).

m) If you convey a User Product that includes a covered work under this section 6, and the work is specifically designed
to work with that User Product, then the Corresponding Source for the work must be accompanied by the Installation
Information, unless installing such modified versions is not permitted in that jurisdiction and cannot be arranged
through contractual obligations with the user.

7. Additional Terms.

"Additional permissions" are terms that supplement the terms of this License by making exceptions from one or more of
its conditions. Additional permissions that are applicable to the entire Program shall be treated as though they were
included in this License, to the extent that they are valid under applicable law. If additional permissions apply only
to part of the Program, that part may be used separately under those permissions, but the entire Program remains
governed by this License without regard to the additional permissions.

When you convey a copy of a covered work, you may at your option remove any additional permissions from that copy, or
from any part of it, provided the relicensing is valid under applicable law.

You may place additional permissions on material, added by you to a covered work, for which you have or can give
appropriate copyright permission.

Notwithstanding any other provision of this License, for material you add to a covered work, you may (if authorized by
the copyright holders of that material) supplement the terms of this License with terms:

    a) Disclaiming warranty or limiting liability differently from the
    terms of sections 15 and 16 of this License; or

    b) Requiring preservation of specified reasonable legal notices or
    author attributions in the material or in the Appropriate Legal
    Notices displayed by works containing it; or

    c) Prohibiting misrepresentation of the origin of that material, or
    requiring that modified versions of such material be marked in
    reasonable ways as different from the original version; or

    d) Limiting the use for publicity purposes of names of the
    contributors or project names of the Program, or of entities
    associated with the Program, except as required for preserving the
    attribution of contributors to the material; or

    e) Declining to grant rights under trademark law for use of some
    trade names, trademarks, or service marks; or

    f) Requiring indemnification of licensors and contributors by
    anyone who conveys the material (or modified versions of it) with
    contractual assumptions of liability to the recipient, for any
    liability that these contractual assumptions directly impose on
    those licensors and contributors.

All other non-permissive additional terms are considered "further restrictions" within the meaning of section 10. If the
Program as you received it purports to apply both this License and any other license, you may choose either license
individually, or both in combination, as the terms of each license permit.

8. Termination.

You may not propagate or modify a covered work except as expressly provided under this License. Any attempt to do so
otherwise is void, and will automatically terminate your rights under this License (including any patent licenses
granted under the third paragraph of section 11).

However, if you cease all violation of this License, then your license from a particular copyright holder is reinstated
(a)
provisionally, unless and until that copyright holder explicitly and finally terminates your license, and (b)
permanently, if the copyright holder fails to notify you of the violation by some reasonable means prior to 60 days
after the cessation.

Moreover, your license from a particular copyright holder is reinstated permanently if that copyright holder notifies
you of the violation by some reasonable means, this is the first time you have received notice of violation from that
copyright holder, and you cure the violation within 30 days following the receipt of the notice.

Termination of your rights under this section does not terminate the licenses of parties who have received copies or
rights from you under this License. If your rights have been terminated and not permanently reinstated, you do not
qualify to receive new licenses for the same material under section 10.

9. Acceptance Not Required for Mere Use.

YOU ARE NOT REQUIRED TO ACCEPT THIS LICENSE IN ORDER TO RECEIVE OR RUN A COPY OF THE PROGRAM. Ancillary propagation of a
covered work occurring solely as a consequence of using peer-to-peer transmission for a network location does not
require acceptance. However, nothing other than this License grants you permission to propagate or modify any covered
work. These actions infringe copyright if you do not accept this License.

Therefore, by modifying or propagating a covered work, you indicate your acceptance of this License to do so.

10. Automatic Licensing of Downstream Recipients.

Each time you convey a covered work, the recipient automatically receives a license from the original licensors, to run,
modify and propagate that work, subject to this License. You are not responsible for enforcing compliance by third
parties with this License.

An "entity transaction" is a transaction transferring control of an organization, or substantially all of the assets of
one, or subdividing an organization, or merging organizations. If propagation of a covered work results from an entity
transaction, each party to that transaction who receives a copy of the work also receives whatever licenses to the work
the party's predecessor in interest had or could give under the previous paragraph, plus a right to possession of the
Corresponding Source of the work from the predecessor in interest, if the predecessor has it or can get it with
reasonable efforts.

You may not impose any further restrictions on the exercise of the rights granted or affirmed under this License. For
example, you may not impose a license fee, royalty, or other charge for exercise of rights granted under this License,
and you may not initiate litigation (including a cross-claim or counterclaim in a lawsuit) alleging that any patent
claim is infringed by making, using, selling, offering for sale, or importing the Program or any portion of it.

11. Patents.

Each contributor grants you a non-exclusive, worldwide, royalty-free patent license under the contributor's essential
patent claims, to make, use, sell, offer for sale, import and otherwise run, modify and propagate the contents of its
contributor version.

In the following paragraphs, a "patent license" is any express agreement or commitment, however denominated, not to
enforce a patent against any party for making, using, selling, offering for sale, importing or running a work covered by
this License.

A "contributor version" means the combination of the Program and any modifications or additions conveyed by that
contributor.

A contributor's "essential patent claims" are all patent claims owned or controlled by the contributor that would be
infringed by some manner, permitted by this License, of making, using, or selling or importing its contributor version,
but do not include claims that would be infringed only as a consequence of further modification of the contributor
version. For purposes of this definition, "control" means all functional equivalents of a patent claim, regardless of
whether they are called patents or are implemented as trade secrets, design rights, or the like.

If you convey a covered work, knowingly relying on a patent license, and the Corresponding Source of the work is not
available for anyone to copy, free of charge and under the terms of this License, through a publicly available network
server or other readily accessible means, then you must either (1) cause the Corresponding Source to be so available, or
(2) arrange to deprive yourself of the benefit of the patent license for this particular work, or (3) arrange, in a
manner consistent with the requirements of this License, to extend the patent license to downstream recipients.
"Knowingly relying" means you have actual knowledge that, but for the patent license, your conveying the covered work in
a country, or your recipient's use of the covered work in a country, would infringe one or more identifiable patents in
that country that you have reason to believe are valid.

If, pursuant to or in connection with a single transaction or arrangement, you convey, or propagate by procuring
conveyance of, a covered work, and grant a patent license to some of the parties receiving the covered work authorizing
them to use, propagate, modify or convey a specific copy of the covered work, then the patent license you grant is
automatically extended to all recipients of the covered work and works based on it.

A patent license is "discriminatory" if it does not cover some parties who received the covered work, or does not cover
some modifications or additions that a recipient could make, solely because of their identity or the nature of their
relationship with you.

If you convey a covered work, knowingly relying on a patent license, and the Corresponding Source is not available as
described above, then you may not convey the work in any country where such conveyance would infringe one or more
identifiable patents in that country that you have reason to believe are valid, unless you have taken steps to ensure
that the Corresponding Source will be available as described above.

12. No Surrender of Others' Freedom.

If conditions are imposed on you (whether by court order, agreement or otherwise) that contradict the conditions of this
License, they do not excuse you from the conditions of this License. If you cannot convey a covered work so as to
satisfy simultaneously your obligations under this License and any other pertinent obligations, then as a consequence
you may not convey it at all. For example, if you agree to terms that obligate you to collect a royalty for further
conveyance from those to whom you convey the Program, the only way you could satisfy both those terms and this License
would be to refrain entirely from conveying the Program.

13. Use with the GNU Affero General Public License.

Notwithstanding any other provision of this License, you have permission to link or combine any covered work with a work
licensed under version 3 of the GNU Affero General Public License into a single combined work, and to convey the
resulting work. The terms of this License will continue to apply to the part which is the covered work, but the work
with which it is combined will remain governed by version 3 of the GNU Affero General Public License.

14. Revised Versions of this License.

The Free Software Foundation may publish revised and/or new versions of the GNU General Public License from time to
time. Such new versions will be similar in spirit to the present version, but may differ in detail to address new
problems or concerns.

Each version is given a distinguishing version number. If the Program specifies that a certain numbered version of the
GNU General Public License "or any later version" applies to it, you have the option of following the terms and
conditions either of that numbered version or of any later version published by the Free Software Foundation. If the
Program does not specify a version number of the GNU General Public License, you may choose any version ever published
by the Free Software Foundation.

If the Program specifies that a proxy can decide which future versions of the GNU General Public License can be used,
that proxy's public statement of acceptance of any version is permanent authorization for you to choose that version for
the Program.

15. Disclaimer of Warranty.

THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW, EXCEPT WHEN OTHERWISE STATED IN WRITING
BY THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDING THE PROGRAM "AS IS"
WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM
IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

16. Limitation of Liability.

IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO
MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL,
INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO
LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM
TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
DAMAGES.

17. Interpretation.

If the disclaimer of warranty and limitation of liability provided above cannot be given local legal effect according to
their terms, reviewing courts shall apply local law that most closely approximates an absolute waiver of all civil
liability in connection with the Program, unless a warranty or assumption of liability accompanies a copy of the Program
in return for a fee.

                     END OF TERMS AND CONDITIONS

            How to Apply These Terms to Your New Programs

If you develop a new program, and you want it to be as free as possible, the best way to achieve this is to make it free
software that everyone can redistribute and change under these terms.

To do so, attach the following notices to the program. It is safest to attach them to the start of each source file to
most effectively state the exclusion of warranty; and each file should have at least the
"copyright" line and a pointer to where the full notice is found.

    <one line to give the program's name and a brief idea of what it does.>
    Copyright (C) 2026  vliantwo  <your@email>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

Also add information on how to contact you by electronic and paper mail.

If the program does terminal interaction, make it output a short notice like this when it starts in an interactive mode:

    <program>  Copyright (C) 2026  vliantwo
    This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
    This is free software, and you are welcome to redistribute it
    under certain conditions; type `show c' for details.

The hypothetical commands `show w' and `show c' should show the appropriate parts of the General Public License. Of
course, your program's commands might be different; for a GUI interface, you would use an "about box".

You should also get your employer (if you work as a programmer) or school, if any, to sign a "copyright disclaimer" for
the program, if necessary. For more information on this, and how to apply and follow the GNU GPL,
see <https://www.gnu.org/licenses/>.

The GNU General Public License does not permit incorporating your program into proprietary programs. If your program is
a software library, you may consider it more useful to permit linking proprietary applications with the library. If this
is what you want to do, use the GNU Lesser General Public License instead of this License. But first, please
read <https://www.gnu.org/licenses/why-not-lgpl.html>.

============================================================================

Credits & Acknowledgements (StarShack specific)

StarShack is derived from Novoline-bS, which itself is based on the Raven-bS / Raven-XD lineage of Minecraft utility
mods. Accordingly, this distribution includes and builds upon code originally authored by the Novoline-bS and Raven-bS
developers. Their contributions are gratefully acknowledged:

- Novoline-bS (https://github.com/Ij1chi-Nijika/Novoline-bS) by Ij1chi-Nijika — the direct base of this project. Much of
  the module/setting architecture, ClickGUI, and Mixin work originates here.

- Raven-bS / Raven-XD — the original "Raven" lineage from which Novoline-bS itself was extended. Core concepts (ESP,
  Aura, Scaffold, etc.) trace back to this community.

This acknowledgment is provided in accordance with the spirit of the GPL, and the original copyright notices and license
terms are preserved in the source where applicable.

============================================================================

            END OF LICENSE FILE