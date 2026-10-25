document.addEventListener("DOMContentLoaded", () => {
  const board = document.getElementById("pipeline-board");
  if (!board) return;

  const feedback = document.getElementById("pipeline-feedback");
  const csrfToken = board.dataset.csrfToken;
  const csrfHeader = board.dataset.csrfHeader;
  const moveBase = board.dataset.moveBase;
  let draggedCard = null;

  const showError = (message) => {
    feedback.textContent = message || "No fue posible mover la oportunidad.";
    feedback.classList.remove("d-none");
    feedback.scrollIntoView({ behavior: "smooth", block: "nearest" });
  };

  board.querySelectorAll(".pipeline-card").forEach((card) => {
    card.addEventListener("dragstart", (event) => {
      if (event.target.closest("a, button, input, select, textarea, form")) {
        event.preventDefault();
        return;
      }

      draggedCard = card;
      card.classList.add("pipeline-card-dragging");
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/plain", card.dataset.opportunityId);
    });
    card.addEventListener("dragend", () => {
      card.classList.remove("pipeline-card-dragging");
      board.querySelectorAll(".pipeline-dropzone-active").forEach((zone) => zone.classList.remove("pipeline-dropzone-active"));
      draggedCard = null;
    });
  });

  board.querySelectorAll(".pipeline-dropzone").forEach((zone) => {
    zone.addEventListener("dragover", (event) => {
      event.preventDefault();
      event.dataTransfer.dropEffect = "move";
      zone.classList.add("pipeline-dropzone-active");
    });
    zone.addEventListener("dragleave", () => zone.classList.remove("pipeline-dropzone-active"));
    zone.addEventListener("drop", async (event) => {
      event.preventDefault();
      zone.classList.remove("pipeline-dropzone-active");
      if (!draggedCard || draggedCard.dataset.currentStage === zone.dataset.stageCode) return;

      feedback.classList.add("d-none");
      const headers = { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" };
      headers[csrfHeader] = csrfToken;
      try {
        const response = await fetch(`${moveBase}/${draggedCard.dataset.opportunityId}/stage`, {
          method: "POST",
          headers,
          body: new URLSearchParams({ stageCode: zone.dataset.stageCode })
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
          showError(payload.message);
          return;
        }
        window.location.reload();
      } catch (error) {
        showError("No fue posible conectar con el servidor. La oportunidad conserva su etapa anterior.");
      }
    });
  });
});
