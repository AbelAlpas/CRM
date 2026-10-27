document.addEventListener("DOMContentLoaded", () => {
  const selects = [...document.querySelectorAll(".activity-relation-select")];
  selects.forEach((select) => {
    select.addEventListener("change", () => {
      if (!select.value) return;
      selects.filter((item) => item !== select).forEach((item) => {
        item.value = "";
      });
    });
  });
});
