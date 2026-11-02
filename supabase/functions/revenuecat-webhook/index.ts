import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const pointsByProduct: Record<string, number> = {
  astrotalks_points_100: 100,
  astrotalks_points_250: 250,
  astrotalks_points_500: 500,
  astrotalks_points_1000: 1000,
};

Deno.serve(async (request) => {
  if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });
  const expected = Deno.env.get("REVENUECAT_WEBHOOK_AUTH");
  if (!expected || request.headers.get("Authorization") !== `Bearer ${expected}`) {
    return new Response("Unauthorized", { status: 401 });
  }

  const payload = await request.json();
  const event = payload?.event;
  const productId = event?.product_id as string | undefined;
  const userId = event?.app_user_id as string | undefined;
  const transactionId = (event?.transaction_id || event?.original_transaction_id) as string | undefined;
  const points = productId ? pointsByProduct[productId] : undefined;
  const supported = new Set(["INITIAL_PURCHASE", "NON_RENEWING_PURCHASE", "TRANSFER"]);
  if (!userId || !transactionId || !points || !supported.has(event?.type)) {
    return Response.json({ received: true, credited: false });
  }

  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data: existing } = await admin.from("payment_transactions").select("id").eq("provider_payment_id", transactionId).maybeSingle();
  if (existing) return Response.json({ received: true, credited: false, duplicate: true });

  const { error: paymentError } = await admin.from("payment_transactions").insert({
    user_id: userId,
    provider: "revenuecat_google_play",
    provider_payment_id: transactionId,
    amount_minor: 1,
    currency: "INR",
    points,
    status: "verified",
  });
  if (paymentError) return Response.json({ error: paymentError.message }, { status: 500 });
  const { error: ledgerError } = await admin.from("points_ledger").insert({
    user_id: userId,
    kind: "purchase",
    points,
    reference_id: transactionId,
  });
  if (ledgerError) return Response.json({ error: ledgerError.message }, { status: 500 });
  return Response.json({ received: true, credited: true, points });
});
