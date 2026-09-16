import Link from "next/link";
import { Gavel, Settings2 } from "lucide-react";

export default function AuctionUnavailable() {
  return (
    <div className="grid min-h-[75vh] place-items-center p-8">
      <div className="card-elev max-w-xl rounded-3xl p-10 text-center">
        <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl border border-primary/25 bg-primary/10 text-primary">
          <Gavel size={30} />
        </div>
        <div className="label-cap mt-6">Auction setup required</div>
        <h1 className="h-heading mt-2 text-3xl">No operational auction yet</h1>
        <p className="mt-4 text-sm leading-6 text-white/50">
          Configure and finalize the championship auction before opening the live
          auction workspace or its bidding history.
        </p>
        <Link href="/competition" className="btn btn-primary mt-7 inline-flex">
          <Settings2 size={16} /> Open configuration
        </Link>
      </div>
    </div>
  );
}
