# Voxelgame workshop: Kliendi-Serveri Autoriteet, Protseduuriline Graafika ja AI

Tegemist on 3D Minecraft-stiilis mängu projektiga, milles on osad funktsionaalsused katki või eemaldatud. Sinu ülesanne on need koodi tagasi lisada, parandada serveri turvalisust ning luua tehisintellekti agendi abil uusi visuaalseid lahendusi.

**NB! Palun tutvu enne töötoaga alustamist esialgse projektiga ja selle README-ga, et teaksid, kuidas mäng töötama peaks.**

## Kontseptid ja Tööriistad

*   **[libGDX wiki](https://libgdx.com/wiki/)**: Raamistik, millele antud mäng toetub.


*   **[OpenGL tutorial](https://learnopengl.com/getting-started/shaders)**: Mäng kasutab OpenGL-i varjutajaid (shaders) graafika renderdamiseks. See tutorial on suurepärane sissejuhatus GLSL-i ja varjutajate loomiseks.


*   **Võrguprogrammeerimise kuldreegel**: *Server on autoriteet.* Klient on ainult "rumal" kuvaja, mis edastab kasutaja soove. Server peab alati valideerima, kas kasutaja tegevus on tegelikult lubatud.


*   **Protseduuriline genereerimine**: Selles projektis pole kasutatud ühtegi pildifaili (`.png`) ega 3D mudelit (`.obj`, `.gltf`). Nii maailm, tekstuurid kui ka mängija mudel luuakse reaalajas koodi (matemaatika ja müra algoritmide) abil. Uuri klasse `PlayerModelGenerator`, `TerrainGenerator` ja `Shaders.java`.


### 🤖 Tööriistad: IDE, AI, agent
See projekt on suur — **kasuta julgelt** IDE otsingut, Copilotit, Cursorit või muud assistenti koodibaasis liikumiseks ja ülesannete lahendamiseks. Iga ülesande juures on lühike **„Alusta siit“** (orienteerumiseks); täpsemad detailid on vihjetes.

*   **Näide:** *"Kus serveris töödeldakse `BlockChangeMessage` ja kuidas leian sealt mängija?"*

---

## Eemaldatud ja katkised funktsionaalsused

**NB!** Enne ja pärast iga funktsionaalsuse lisamist pane projekt tööle. Kaitsmisel arvestatakse ainult korrektselt töötavate lahendustega!

> Loe ülesanne → kasuta IDE/AI abi → **„Alusta siit“** kui vajad suunda → vihjed/lahendus, kui jäid hätta.

### 1. "New Game" või "Join Game" nupule vajutades avaneb mäng
Hetkel vajutades menüüs nuppe, mäng ei alga – ekraan jääb menüüsse, kuigi taustal serveriga ühendus luuakse.

**Alusta siit:** `core/.../screen/TitleScreen.java`

<details> 
<summary>💡 Vihje 1</summary> 

Muudatus tuleb teha **kliendi klassis** `TitleScreen`. Nuppude defineerimisel (`continueButton` ja `newGameButton`) saadetakse serverile sõnum, aga ekraani ennast ei vahetata.
</details> 


<details> 
<summary>💡 Vihje 2</summary> 

Kliendi ekraanide vahetamiseks kasutatakse `game.setScreen(...)` meetodit, kus `game` on `VoxelGame` klassi instants. Sinu ülesanne on leida õige koht, kuhu see lisada, nii et peale nupuvajutust vahetuks ekraan 3D mängu vaate vastu.
</details> 


<details> 
<summary>💡 Vihje 3</summary> 

Vajaliku ekraani klass on `VoxelScreen`, mis asub paketis `screen`. Selle konstruktorisse tuleb edastada `game` objekt.
</details> 


<details> 
<summary>🛠 Lahendus</summary> 

Mõlema nupu lambda-avaldise lõppu tuleb lisada ekraani vahetus 3D vaate vastu:
```java 
game.setScreen(new VoxelScreen(game)); 
``` 
</details>

### 2. Gravitatsioon on katki (Mängija ei saa hüpata)
Tühikut vajutades tegelane ei hüppa. Konstandid on katki läinud.

**Alusta siit:** `shared/.../constant/Constants.java`

<details> 
<summary>💡 Vihje 1</summary> 

Kõik füüsika ja mängija liikumisega seotud numbrid on defineeritud ühes kindlas klassis `constant` paketis.
</details>

<details> 
<summary>🛠 Lahendus</summary> 

Ava klass `Constants` ja paranda muutuja `JUMP_VELOCITY` funktsionaalseks:
```java 
public static final float JUMP_VELOCITY = 10f; 
``` 
</details>

### 3. "Lendava häkkeri" peatamine (Server-side validation)
Praegu saab klient vajutada 'F' klahvi ning saata serverile sõnumi `fly = true`. Server usaldab seda ja lubab lennata! Sinu ülesanne on muuta serveri loogikat nii, et lendamine poleks lubatud (ignoreeri kliendi `fly` väärtust). Pärast parandust võib klient endiselt *arvata*, et ta lendab, aga tegelikult server ei luba. 

**Alusta siit:** `server/.../game/object/Player.java`

<details> 
<summary>💡 Vihje 1</summary>

Kliendi sisend jõuab serverisse sõnumina `PlayerMovementMessage`, millel on väli `fly`. Serveri poolel tuleb leida koht, kus seda sõnumit töödeldakse, ja veenduda, et `fly` väärtust ei kasutata mängija liikumise mõjutamiseks.
</details>

<details> 
<summary>💡 Vihje 2</summary>

Kliendi sisend jõuab serverisse läbi klassi `PlayerMovementListener`, mis kutsub välja `Player` klassi meetodi `handleInput(PlayerMovementMessage message)`.
</details>

<details> 
<summary>🛠 Lahendus</summary> 

Muuda `Player.java` meetodit `handleInput` nii, et lendamise *boolean* seadistatakse alati väärtusele `false`.
```java
// Kliendi "fly" soovi ignoreeritakse, server on autoriteet!
inputState.setFly(false); 
```

**Kas märkad pärast parandust vibreerimist, kui `F` on kliendis sisse lülitatud?**

See on oodatud ja õpetlik: klient ennustab endiselt lendamist (`VoxelScreen` kasutab `inputManager.isFly()`), aga server seda ei luba. Mängija nüüd **ei saa tegelikult lennata** — autoriteetne positsioon tuleb serverist ja `synchronizeLocalPlayerWithServer` korrigeerib klienti. Vibreerimine näitab seda lahknevust. Kui tahad sujuvat kuva ilma lennuta, lülita `F` välja.
</details>

❗Pärast ülesande lahendamist taasta lendamise esialgne seis, järgmistes ülesannetes on lendamisoskus kindlasti kasuks!

### 4. Pika käega ehitaja (Kauguse valideerimine)
Häkkerist klient võiks saata `BlockChangeMessage` sõnumi, et ta ehitab ploki 1000 ühikut eemale. Server peab kontrollima, kas mängija asub ehitatavast plokist piisavalt lähedal (näiteks maksimaalselt 6 ühiku kaugusel).

Vajuta mängus olles klahvi **R**. See käivitab kliendis näidiskoodi, mis proovib ehitada plokkidest ringi mängijast 15 ühiku kaugusele.

Enne parandust peaksid nägema, et kaugele tekivad plokid; pärast parandust peaks server need ehituspäringud ignoreerima, kuna need on kaugemal kui lubatud piir (6 ühikut).

**Alusta siit:** `server/.../game/GameInstance.java`

<details> 
<summary>💡 Vihje 1</summary> 

Serveris tegeleb plokkide muutmisega `GameInstance.handleBlockChange(Connection connection, int x, int y, int z, int blockType)`. Mängija leidmiseks kasuta samas klassis olevat abimeetodit `getPlayerByConnection(connection)`.
</details>

<details> 
<summary>💡 Vihje 2</summary> 

NB! Server ise muudab ka plokke (näiteks voolav vesi või langev liiv). Sellistel puhkudel on `connection` väärtus `null`. Ära unusta seda kontrollida!
</details>

<details> 
<summary>💡 Vihje 3 (Mängija asukoht ja geomeetria)</summary> 

Mängija koordinaadid saad kätte läbi tema füüsikaoleku: `player.getPhysicsState().getX()` (analoogselt Y ja Z).
Kahe punkti vahelise 3D-kauguse arvutamiseks kasuta klassikalist valemit: d = √((x₂ - x₁)² + (y₂ - y₁)² + (z₂ - z₁)²). Javas on selleks abiks `Math.sqrt()` ja `Math.pow()`.
</details>

<details> 
<summary>🛠 Lahendus</summary> 

Leia mängija, arvuta 3D distants mängija koordinaatide (`physicsState`) ja ploki koordinaatide vahel ning ignoreeri ehitust (tee `return;`), kui vahemaa on liiga suur.
```java
public synchronized void handleBlockChange(Connection connection, int x, int y, int z, int blockType) {
    if (connection != null) {
        Player player = getPlayerByConnection(connection);

        if (player != null) {
            float px = player.getPhysicsState().getX();
            float py = player.getPhysicsState().getY();
            float pz = player.getPhysicsState().getZ();

            double distance = Math.sqrt(Math.pow(x - px, 2) + Math.pow(y - py, 2) + Math.pow(z - pz, 2));
            if (distance > 6.0) {
                return; // Mängija on liiga kaugel, ignoreeri ehitust!
            }
        }
    }
    // ... originaalne kood jätkub ...
```
</details>

### 5. Serveri ülekoormamine chunkidega (DoS rünnaku ennetus)
Pahatahtlik klient võib saata tuhandeid `ChunkRequestMessage` päringuid koordinaatidele, mis asuvad maailma lõpus, koormates seeläbi serveri üle. Teeme nii, et server kontrollib, et mängija küsiks *chunke* ainult enda lähedalt (nt mitte kaugemalt kui 5 chunki raadiuses).

Vajuta mängus olles klahvi **T**. See käivitab kliendis näidiskoodi, mis püüab serverilt küsida korraga tervet "ringi" chunke, mis asuvad mängijast 14 chunki kaugusel.

Enne parandust hakkab server neid kaugeid chunke genereerima, mis koormab serverit, sest tegelikult ei tohiks klient nii kaugeid chunke küsida.

Pärast parandust peaks server need päringud ignoreerima.

**Alusta siit:** `server/.../game/GameInstance.java`

<details> 
<summary>💡 Vihje 1 (Mängija tuvastamine)</summary> 

Vaata meetodit `GameInstance.handleChunkRequest`. Sarnaselt eelmise ülesandega kasuta mängija tuvastamiseks ühenduse järgi abimeetodit `getPlayerByConnection(connection)`.
</details>

<details> 
<summary>💡 Vihje 2 (Koordinaatide teisendamine)</summary> 

Mängija asukoht (`player.getPhysicsState().getX()` ja `getZ()`) on antud maailma koordinaatides, kuid päring saabub chunk'i koordinaatides (`chunkX` ja `chunkZ`).

Mängija asukoha teisendamiseks chunk'i koordinaatideks jaga see chunk'i suurusega (`Chunk.SIZE_X` ja `Chunk.SIZE_Z`) ning ümarda tulemus alla lähima täisarvuni kasutades `Math.floor` meetodit. Ära unusta tulemust tüübiteisendada `int` tüübiks.
</details>

<details> 
<summary>💡 Vihje 3 (Lubatud vahemaa kontroll)</summary> 

Nüüd pead võrdlema mängija praegust chunk'i (`pCx`, `pCz`) ja küsitud chunk'i (`chunkX`, `chunkZ`).

Kuna mängija saab liikuda igas suunas, kasuta kahe koordinaadi erinevuse leidmiseks absoluutväärtuse funktsiooni `Math.abs()`. Kui erinevus X- või Z-teljel on suurem kui 5, tähendab see, et klient küsib liiga kaugeid chunke ja päringut tuleks ignoreerida (`return;`).
</details>

<details> 
<summary>🛠 Lahendus</summary> 

```java
public synchronized void handleChunkRequest(Connection connection, int chunkX, int chunkZ) {
    Player player = getPlayerByConnection(connection);

    if (player != null) {
        int pCx = (int) Math.floor(player.getPhysicsState().getX() / Chunk.SIZE_X);
        int pCz = (int) Math.floor(player.getPhysicsState().getZ() / Chunk.SIZE_Z);

        if (Math.abs(pCx - chunkX) > 5 || Math.abs(pCz - chunkZ) > 5) {
            return; // Ignoreeri ebaseaduslikku päringut
        }
    }
    // ... algne chunk'i genereerimise kood ...
```
</details>

### 6. Puulehtede läbipaistvaks muutmine (Shaders & Meshing)
Hetkel on puulehed (Leaves) paksud ja läbipaistmatud rohelised plokid. Kuna graafika renderdatakse protseduuriliselt koodis, on sinu ülesanne muuta puulehed (`BlockConstants.MAT_LEAVES`) läbipaistvaks (auguliseks).

**Alusta siit:** `core/.../game/` (mesh ja shader)

Hea lähtepunkt AI-le — *prompt*:
*"Selles Java + LibGDX vokselmängus genereeritakse 3D meshid failis `VoxelMeshBuilder.java` ja värvitakse GLSL-is failis `Shaders.java`. Puulehtede ID on 5 (`BlockConstants.MAT_LEAVES`). Ma soovin muuta puulehed varjutajas auguliseks (alpha cutout / discard) kasutades olemasolevat noise funktsiooni. Mida ma pean nendes kahes failis muutma?"*

<details> 
<summary>💡 Vihje 1 (AI pime nurk)</summary> 

Tõenäoliselt annab AI sulle ideaalse GLSL koodi, kus kasutatakse märksõna `discard;` (mis jätab piksli joonistamata). Kui sa selle koodi lisad, avastad, et lehtedesse tekivad augud, **aga aukude taga on tühjus, mitte teised plokid!**

**Miks?** Sest vokselmootor peidab jõudluse säästmiseks ära kõik sisemised tahud (*face culling*). Mootor arvab endiselt, et puuleht on täielikult läbipaistmatu.
</details>

<details> 
<summary>💡 Vihje 2</summary> 

Tahkude joonistamise otsus tehakse klassis `VoxelMeshBuilder.java` meetodis `shouldDrawFace`. Pead süsteemile ütlema, et kui naaberplokk (`nMat`) on puuleht, siis ei tohi praegust tahku peita, sest me näeme puulehest läbi!
</details>

<details> 
<summary>🛠 Lahendus</summary> 

**1. Java koodi muudatus (`VoxelMeshBuilder.java`):**
Leia meetod `shouldDrawFace` ja uuenda viimast tagastust:
```java
// Transparent vs opaque => visible
return nMat == BlockConstants.MAT_WATER || nMat == BlockConstants.MAT_GLASS || nMat == BlockConstants.MAT_LEAVES;
```

**2. GLSL Shaderi muudatus (`Shaders.java` -> FRAG koodiplokk):**
```glsl
else if (id == 5) { // LEAVES
    float n = noiseSmall(pos.xz * 1.2 + pos.y * 2.0); // lisame Y telje
    if (n > 0.55) discard; // Tekitame augu!
    return clamp(u_mat_leaves + vec3(n * 0.12), 0.0, 1.0);
}
```
</details>

### 7. BOONUS: Bioomid ja reljeefi parandamine
Kui vaatad maailma maastikku, märkad, et see on ebaloomulikult regulaarne – justkui lõputu "munarest". Põhjus on selles, et failis `TerrainGenerator.java` arvutatakse maastiku kõrgus (`calculateSurfaceHeight`) primitiivsete `Math.sin` ja `Math.cos` funktsioonide abil.

**Alusta siit:** `shared/.../game/TerrainGenerator.java`

**Sinu ülesanne:** Kirjuta parem maastikugeneratsioon (nt *Perlin noise* bioomide jaoks). Anna see mugavalt AI agendile.

<details>
<summary>💡 Vihje</summary>
Keerulisema mitme bioomiga süsteemi loomine on täiesti vabatahtlik, aga isegi lihtne müra-põhine kõrguskaardi parandamine muudaks maailma palju huvitavamaks!

Näiteks kirjuta AI agendile lühidalt:

Meetodi `calculateSurfaceHeight` genereeritud maastik on igav ja korduv. Tee see ilusamaks ja efektiivsemaks, et tekiksid mäed ja orud.
</details>

<br><br>
