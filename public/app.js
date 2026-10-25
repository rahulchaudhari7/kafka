const productGrid = document.querySelector("#product-grid");
const searchInput = document.querySelector("#product-search");
const cartCount = document.querySelector(".cart-count");
const cartButton = document.querySelector(".cart-button");
const toast = document.querySelector("#toast");

const productDetails = {
  1: { category: "Paper goods", icon: "▤", description: "A fresh page for your next big idea." },
  2: { category: "Desk essentials", icon: "✎", description: "A smooth-writing favorite, every day." }
};

let products = [];
let itemCount = 0;
let toastTimeout;

function renderProducts(query = "") {
  const normalizedQuery = query.trim().toLowerCase();
  const filteredProducts = products.filter((product) =>
    product.name.toLowerCase().includes(normalizedQuery)
  );

  productGrid.replaceChildren();
  if (filteredProducts.length === 0) {
    const message = document.createElement("p");
    message.className = "status-message";
    message.textContent = normalizedQuery
      ? "No products match your search. Try another name."
      : "No products are available right now.";
    productGrid.append(message);
    return;
  }

  for (const product of filteredProducts) {
    const details = productDetails[product.id] ?? {
      category: "Everyday goods",
      icon: "✳",
      description: "A thoughtful little something for your day."
    };
    const card = document.createElement("article");
    card.className = "product-card";

    const visual = document.createElement("div");
    visual.className = "product-visual";
    visual.setAttribute("aria-hidden", "true");

    const tag = document.createElement("span");
    tag.className = "product-tag";
    tag.textContent = details.category;

    const icon = document.createElement("span");
    icon.className = "product-icon";
    icon.textContent = details.icon;
    visual.append(tag, icon);

    const info = document.createElement("div");
    info.className = "product-info";
    const description = document.createElement("div");
    const name = document.createElement("h3");
    name.textContent = product.name;
    const summary = document.createElement("p");
    summary.textContent = details.description;
    description.append(name, summary);

    const price = document.createElement("span");
    price.className = "product-price";
    price.textContent = new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD"
    }).format(product.price);
    info.append(description, price);

    const addButton = document.createElement("button");
    addButton.className = "add-button";
    addButton.type = "button";
    addButton.textContent = "Add to bag";
    addButton.setAttribute("aria-label", `Add ${product.name} to bag`);
    addButton.addEventListener("click", () => addToBag(product.name));

    card.append(visual, info, addButton);
    productGrid.append(card);
  }
}

function addToBag(productName) {
  itemCount += 1;
  cartCount.textContent = String(itemCount);
  cartButton.setAttribute("aria-label", `Shopping bag, ${itemCount} items`);
  toast.textContent = `${productName} added to your bag`;
  toast.classList.add("is-visible");
  clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => toast.classList.remove("is-visible"), 2200);
}

async function loadProducts() {
  try {
    const response = await fetch("/api/products");
    if (!response.ok) {
      throw new Error(`Product request failed with status ${response.status}`);
    }

    products = await response.json();
    renderProducts(searchInput.value);
  } catch (error) {
    console.error("Unable to load products:", error);
    productGrid.replaceChildren();
    const message = document.createElement("p");
    message.className = "status-message";
    message.textContent = "We couldn't load the catalog. Please refresh to try again.";
    productGrid.append(message);
  }
}

searchInput.addEventListener("input", () => renderProducts(searchInput.value));
loadProducts();
