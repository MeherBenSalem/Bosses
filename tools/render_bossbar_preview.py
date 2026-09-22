"""Render the actual procedural HUD code against a tiny Java graphics adapter for layout QA."""
from pathlib import Path
import subprocess
import tempfile

root=Path(__file__).resolve().parents[1]
out=root/'docs/hollow-sovereign';out.mkdir(exist_ok=True)
jdk=Path('C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot/bin')
stubs={
'com/nightbeam/remnants/config/JaumlConfigLib.java':'''package com.nightbeam.remnants.config; public class JaumlConfigLib { public static double getNumberValue(String a,String b,String c){return 1;} }''',
'net/minecraft/network/chat/contents/TranslatableContents.java':'''package net.minecraft.network.chat.contents; public record TranslatableContents(String key) { public String getKey(){return key;} }''',
'net/minecraft/network/chat/Component.java':'''package net.minecraft.network.chat; import java.util.*; import net.minecraft.network.chat.contents.TranslatableContents; public class Component { private String key; public Component(String key){this.key=key;} public Object getContents(){return new TranslatableContents(key);} public List<Component> getSiblings(){return List.of();} }''',
'net/minecraft/world/BossEvent.java':'''package net.minecraft.world; import net.minecraft.network.chat.Component; public class BossEvent { private Component name; private float progress; public BossEvent(String key,float progress){name=new Component(key);this.progress=progress;} public Component getName(){return name;} public float getProgress(){return progress;} }''',
'net/minecraft/client/gui/GuiGraphics.java':'''package net.minecraft.client.gui; import java.awt.*; public class GuiGraphics { public Graphics2D g; public GuiGraphics(Graphics2D g){this.g=g;} public void fill(int x,int y,int x2,int y2,int color){g.setColor(new Color(color,true));g.fillRect(x,y,x2-x,y2-y);} }''',
'Preview.java':'''import java.awt.*; import java.awt.image.*; import javax.imageio.*; import java.io.*; import com.nightbeam.remnants.client.RemnantBossBars; import net.minecraft.client.gui.GuiGraphics; import net.minecraft.world.BossEvent;
public class Preview { public static void main(String[] args)throws Exception {
BufferedImage im=new BufferedImage(900,720,BufferedImage.TYPE_INT_ARGB); Graphics2D g=im.createGraphics();g.setColor(new Color(0x151921));g.fillRect(0,0,900,720);g.scale(3,3); GuiGraphics gui=new GuiGraphics(g);
String[] names={"Ossukage","Kotsukage","Umbrakar","Hollow Sovereign"}; String[] keys={"ossukage","kotsukage","umbrakar","hollow_sovereign"};
g.setFont(new Font("Monospaced",Font.BOLD,9));
for(int i=0;i<4;i++){int y=24+i*52;g.setColor(new Color(0xf1e1cc));int w=g.getFontMetrics().stringWidth(names[i]);g.drawString(names[i],150-w/2,y-2);boolean ok=RemnantBossBars.draw(gui,59,y+7,new BossEvent("entity.remnant_bosses."+keys[i],.76f-i*.13f));if(!ok)throw new AssertionError(keys[i]);}
if(RemnantBossBars.theme(new net.minecraft.network.chat.Component("entity.minecraft.wither"))!=-1)throw new AssertionError("Vanilla overridden");g.dispose();ImageIO.write(im,"png",new File(args[0])); } }'''
}
with tempfile.TemporaryDirectory(prefix='bossbar-qa-') as temp:
    temp=Path(temp)
    for name,source in stubs.items():
        p=temp/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source)
    p=temp/'com/nightbeam/remnants/client/RemnantBossBars.java';p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text((root/'1.21.1/common/src/main/java/com/nightbeam/remnants/client/RemnantBossBars.java').read_text())
    subprocess.run([str(jdk/'javac.exe'),'-d',str(temp),*[str(p) for p in temp.rglob('*.java')]],check=True)
    subprocess.run([str(jdk/'java.exe'),'-cp',str(temp),'Preview',str(out/'bossbars.png')],check=True)
print(out/'bossbars.png')
