"use client";
import {useState,useEffect} from "react";
import Image from "next/image";
import {api,type PlayerDto} from "@/lib/api";
import {Dialog} from "@/components/ui/Dialog";
import {Button} from "@/components/ui";
import {PlayerAvatar} from "./PlayerAvatar";
export default function PlayerPhotoDialog({player,championshipId,onClose,onSaved}:{player:PlayerDto;championshipId:string;onClose:()=>void;onSaved:()=>void}){
 const [file,setFile]=useState<File|null>(null),[preview,setPreview]=useState(""),[error,setError]=useState(""),[busy,setBusy]=useState(false),[progress,setProgress]=useState(0),[remove,setRemove]=useState(false);
 useEffect(()=>()=>{if(preview)URL.revokeObjectURL(preview);},[preview]);
 const select=(selected?:File)=>{setError("");setRemove(false);setPreview("");if(selected&&(selected.size>5242880||!["image/jpeg","image/png"].includes(selected.type))){setFile(null);setError("Choose a JPEG or PNG up to 5 MB.");return;}setFile(selected||null);if(selected)setPreview(URL.createObjectURL(selected));};
 const save=async()=>{if(!file&&!remove)return;setBusy(true);setError("");setProgress(0);try{const path=`/championships/${championshipId}/admin/players/${player.id}/photo`;if(remove)await api.delete(path);else{const form=new FormData();form.append("file",file!);await api.put(path,form,{timeout:60000,onUploadProgress:event=>setProgress(Math.round((event.loaded/(event.total||file!.size))*100))});}onSaved();}catch(e:any){setError(e?.response?.data?.message||"Could not save photograph. Please retry.");}finally{setBusy(false);}};
 return <Dialog title={player.photoUrl?"Change player photograph":"Add player photograph"} description={player.fullName} onClose={()=>{if(!busy)onClose();}}><div className="space-y-5">
  <div className="flex justify-center">{preview&&!remove?<Image unoptimized width={256} height={256} src={preview} alt={`Preview for ${player.fullName}`} className="h-48 w-48 rounded-2xl object-cover"/>:<PlayerAvatar name={player.fullName} imageUrl={remove?null:player.photoUrl} size="xl" shape="square"/>}</div>
  <p className="text-sm text-muted">Use a player-supplied or administrator-authorized image. JPEG or PNG, up to 5 MB, 4096 pixels per side and 12 megapixels. The centre square is saved at 512 × 512 pixels; camera metadata is removed.</p>
  <label htmlFor="player-photo-file" className="field-label">Photograph</label><input id="player-photo-file" type="file" accept="image/jpeg,image/png" disabled={busy} onChange={e=>select(e.target.files?.[0])} className="block w-full text-sm"/>
  {remove&&<p role="status" className="text-sm">The photograph will be removed when you save. The default avatar will appear.</p>}{error&&<p role="alert" className="text-sm text-danger">{error}</p>}{busy&&<div role="status"><p className="text-sm">{remove?"Removing photograph…":progress<100?`Uploading ${progress}%` :"Processing photograph…"}</p><progress aria-label="Photo upload progress" value={progress} max={100} className="w-full"/></div>}
  <div className="flex flex-wrap justify-end gap-3">{player.photoUrl&&<Button variant="danger" disabled={busy} onClick={()=>{setRemove(true);setFile(null);setPreview("");setError("");}}>Remove Photo</Button>}<Button variant="ghost" disabled={busy} onClick={onClose}>Cancel</Button><Button loading={busy} disabled={busy||(!file&&!remove)} onClick={save}>Save Changes</Button></div>
 </div></Dialog>;
}
